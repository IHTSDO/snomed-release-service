package org.ihtsdo.buildcloud.service;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Execution.Status;
import org.ihtsdo.buildcloud.entity.ExecutionReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.*;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.Rf2FileExportRunner;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.ihtsdo.buildcloud.service.execution.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.execution.transform.TransformationService;
import org.ihtsdo.buildcloud.service.execution.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.ihtsdo.buildcloud.service.execution.RF2Constants.README_FILENAME_EXTENSION;
import static org.ihtsdo.buildcloud.service.execution.RF2Constants.README_FILENAME_PREFIX;

@Service
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Autowired
	private ExecutionDAO dao;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	@Autowired
	private PreconditionManager preconditionManager;

	@Autowired
	private ReadmeGenerator readmeGenerator;

	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private TransformationService transformationService;

	@Autowired
	private Integer fileProcessingFailureMaxRetry;

	@Autowired
	private String releaseValidationFrameworkUrl;

	@Autowired
	private Boolean offlineMode;

	@Autowired
	private Boolean localRvf;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private RF2ClassifierService classifierService;

	@Override
	public Execution createExecutionFromProduct(final String releaseCenterKey, final String productKey) throws BusinessServiceException {
		final Date creationDate = new Date();
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product.getEffectiveTime() == null) {
			throw new BadConfigurationException("Product effective time must be set before an execution is created.");
		}
		Execution execution;
		try {
			synchronized (productKey) {
				// Do we already have an execution for that date?
				final Execution existingExecution = getExecution(product, creationDate);
				if (existingExecution != null) {
					throw new EntityAlreadyExistsException("An Execution for product " + productKey + " already exists with execution id " + existingExecution.getId());
				}
				execution = new Execution(creationDate, product);
				// Create Product config export
				final String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);
				// save execution with config
				dao.save(execution, jsonConfig);
				MDC.put(MDC_EXECUTION_KEY, execution.getUniqueId());
				LOGGER.info("Created execution.", productKey, execution.getId());
				// Copy all files from Product input and manifest directory to Execution input and manifest directory
				dao.copyAll(product, execution);
			}
			if (!product.isJustPackage()) {
				// Perform Pre-condition testing
				final Status preStatus = execution.getStatus();
				runPreconditionChecks(execution);
				final Status newStatus = execution.getStatus();
				if (newStatus != preStatus) {
					dao.updateStatus(execution, newStatus);
				}
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to create execution.", e);
		} finally {
			MDC.remove(MDC_EXECUTION_KEY);
		}
		return execution;
	}

	@Override
	public Execution triggerExecution(String releaseCenterKey, final String productKey, final String executionId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);

		// Start the execution telemetry stream. All future logging on this thread and it's children will be captured.
		TelemetryStream.start(LOGGER, dao.getTelemetryExecutionLogFilePath(execution));
		LOGGER.info("Trigger product", productKey, executionId);

		try {
			updateStatusWithChecks(execution, Status.BUILDING);

			// Run product
			ExecutionReport report = execution.getExecutionReport();
			String resultStatus = "completed";
			String resultMessage = "Process completed successfully";
			try {
				executeProduct(execution);
			} catch (final BusinessServiceException e) {
				resultStatus = "fail";
				resultMessage = "Failure while processing execution " + execution.getUniqueId() + " due to: "
						+ e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
				LOGGER.warn(resultMessage, e);
			}
			report.add("Progress Status", resultStatus);
			report.add("Message", resultMessage);
			dao.persistReport(execution); // TODO: Does this work?

			updateStatusWithChecks(execution, Status.BUILT);
		} finally {
			// Finish the telemetry stream. Logging on this thread will no longer be captured.
			TelemetryStream.finish(LOGGER);
		}

		return execution;
	}

	@Override
	public List<Execution> findAllDesc(String releaseCenterKey, final String productKey) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}

		return dao.findAllDesc(product);
	}

	@Override
	public Execution find(String releaseCenterKey, final String productKey, final String executionId) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);

		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}

		return dao.find(product, executionId);
	}

	@Override
	public String loadConfiguration(String releaseCenterKey, final String productKey, final String executionId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		try {
			return dao.loadConfiguration(execution);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to load configuration.", e);
		}
	}

	private void updateStatusWithChecks(Execution execution, Status newStatus) throws BadConfigurationException {
		// Assert status workflow position
		switch (newStatus) {
			case BUILDING :
				dao.assertStatus(execution, Status.BEFORE_TRIGGER);
				break;
			case BUILT :
				dao.assertStatus(execution, Status.BUILDING);
				break;
		}

		dao.updateStatus(execution, newStatus);
	}

	@Override
	public InputStream getOutputFile(String releaseCenterKey, final String productKey, final String executionId, final String outputFilePath) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.getOutputFileStream(execution, outputFilePath);
	}

	@Override
	public List<String> getOutputFilePaths(String releaseCenterKey, final String productKey, final String executionId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.listOutputFilePaths(execution);
	}

	@Override
	public InputStream getInputFile(String releaseCenterKey, final String productKey, final String executionId, final String inputFilePath) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.getInputFileStream(execution, inputFilePath);
	}

	@Override
	public List<String> getInputFilePaths(String releaseCenterKey, final String productKey, final String executionId) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.listInputFileNames(execution);
	}

	@Override
	public InputStream getLogFile(String releaseCenterKey, final String productKey, final String executionId, final String logFileName) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.getLogFileStream(execution, logFileName);
	}

	@Override
	public List<String> getLogFilePaths(String releaseCenterKey, final String productKey, final String executionId) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(releaseCenterKey, productKey, executionId);
		return dao.listExecutionLogFilePaths(execution);
	}

	private void runPreconditionChecks(final Execution execution) {
	    LOGGER.info("Start of Pre-condition checks");
		final List<PreConditionCheckReport> preConditionReports = preconditionManager.runPreconditionChecks(execution);
		execution.setPreConditionCheckReports(preConditionReports);
		// analyze report to check whether there is fatal error for all packages
		for (PreConditionCheckReport report : preConditionReports) {
			if (report.getResult() == State.FATAL) {
				// Need to alert release manager of fatal pre-condition check error.
				execution.setStatus(Status.FAILED_PRE_CONDITIONS);
				LOGGER.warn("Fatal error occurred during pre-condition checks, execution {} will be halted.", execution.getId());
				break;
			}
		}
		LOGGER.info("End of Pre-condition checks");
	}

	private void executeProduct(final Execution execution) throws BusinessServiceException {
		LOGGER.info("Start execution {}", execution.getUniqueId());
		Product product = execution.getProduct();

		checkManifestPresent(execution);

		if (product.isJustPackage()) {
			copyFilesForJustPackaging(execution);
		} else {
			final Map<String, TableSchema> inputFileSchemaMap = getInputFileSchemaMap(execution);
			transformationService.transformFiles(execution, inputFileSchemaMap);

			// Convert Delta input files to Full, Snapshot and Delta release files
			Rf2FileExportRunner generator = new Rf2FileExportRunner(execution, dao, uuidGenerator, fileProcessingFailureMaxRetry);
			generator.generateReleaseFiles();

			if (product.isCreateInferredRelationships()) {
				// Run classifier against concept and stated relationship snapshots to produce inferred relationship snapshot
				String relationshipSnapshotOutputFilename = classifierService.generateInferredRelationshipSnapshot(execution, inputFileSchemaMap);
				if (relationshipSnapshotOutputFilename != null) {
					generator.generateDeltaAndFullFromSnapshot(relationshipSnapshotOutputFilename);
				}
			} else {
				LOGGER.info("Skipping inferred relationship creation due to product configuration.");
			}
		}

		// Generate readme file
		generateReadmeFile(execution);

		File zipPackage;
		try {
			final Zipper zipper = new Zipper(execution, dao);
			zipPackage = zipper.createZipFile();
			LOGGER.info("Start: Upload zipPackage file {}", zipPackage.getName());
			dao.putOutputFile(execution, zipPackage, true);
			LOGGER.info("Finish: Upload zipPackage file {}", zipPackage.getName());
		} catch (JAXBException | IOException | ResourceNotFoundException e) {
			throw new BusinessServiceException("Failure in Zip creation caused by " + e.getMessage(), e);
		}

		String rvfStatus = "N/A";
		String rvfResultMsg = "RVF validation configured to not run.";
		if (!offlineMode || localRvf) {
			try {
				rvfResultMsg = runRVFPostConditionCheck(execution, zipPackage);
				if (rvfResultMsg == null) {
					rvfStatus = "Completed without errors.";
				} else {
					rvfStatus = "Completed, errors detected.";
				}
			} catch (final Exception e) {
				LOGGER.error("Failure during RVF Post Condition Testing", e);
				rvfStatus = "Processing Failed.";
				rvfResultMsg = "Failure due to: " + e.getLocalizedMessage();
			}
		}
		final ExecutionReport report = execution.getExecutionReport();
		report.add("Post Validation Status", rvfStatus);
		report.add("RVF Test Failures", rvfResultMsg);

		LOGGER.info("End of running execution {}", execution.getUniqueId());
	}

	private void checkManifestPresent(Execution execution) throws BusinessServiceException {
		try {
			final InputStream manifestStream = dao.getManifestStream(execution);
			if (manifestStream == null) {
				throw new BadConfigurationException("Failed to find valid manifest file.");
			} else {
				manifestStream.close();
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to close manifest file.", e);
		}
	}

	private String runRVFPostConditionCheck(final Execution execution, final File zipPackage) throws IOException,
			PostConditionException {
	    	LOGGER.info("Run RVF post-condition check for zip file {}", zipPackage.getName());
		String logFilename = "postcheck-rvf-" + zipPackage.getName() + ".log";
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl);
			 AsyncPipedStreamBean logFileOutputStream = dao.getLogFileOutputStream(execution, logFilename)) {

			return rvfClient.checkOutputPackage(zipPackage, logFileOutputStream);
		}
	}

	private void copyFilesForJustPackaging(final Execution execution) {
		LOGGER.info("Just copying files in execution {} for packaging", execution.getUniqueId());

		// Iterate each execution input file
		final List<String> executionInputFilePaths = dao.listInputFileNames(execution);
		for (final String relativeFilePath : executionInputFilePaths) {
			dao.copyInputFileToOutputFile(execution, relativeFilePath);
		}
	}

	private Map<String, TableSchema> getInputFileSchemaMap(Execution execution) throws BusinessServiceException {
		final List<String> executionInputFilePaths = dao.listInputFileNames(execution);
		final Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		for (final String executionInputFilePath : executionInputFilePaths) {
			final TableSchema schemaBean;
			try {
				schemaBean = schemaFactory.createSchemaBean(FileUtils.getFilenameFromPath(executionInputFilePath));
			} catch (FileRecognitionException e) {
				throw new BusinessServiceException("Did not recognise input file '" + executionInputFilePath + "'", e);
			}
			inputFileSchemaMap.put(executionInputFilePath, schemaBean);
		}
		return inputFileSchemaMap;
	}

	private Execution getExecutionOrThrow(String releaseCenterKey, final String productKey, final String executionId) throws ResourceNotFoundException {
		final Execution execution = getExecution(releaseCenterKey, productKey, executionId);
		if (execution == null) {
			throw new ResourceNotFoundException("Unable to find execution for releaseCenterKey: " + releaseCenterKey + ", productKey: " + productKey + ", executionId: " + executionId);
		}
		return execution;
	}

	private Execution getExecution(String releaseCenterKey, final String productKey, final String executionId) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
		return dao.find(product, executionId);
	}

	private Execution getExecution(final Product product, final Date creationTime) {
		return dao.find(product, EntityHelper.formatAsIsoDateTime(creationTime));
	}
	

	private Product getProduct(String releaseCenterKey, final String productKey) throws ResourceNotFoundException {
		return productDAO.find(releaseCenterKey, productKey, SecurityHelper.getRequiredUser());
	}

	private void generateReadmeFile(final Execution execution) throws BusinessServiceException {
		try {
			LOGGER.info("Generating readMe file for execution {}", execution.getUniqueId());
			final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
			final InputStream manifestStream = dao.getManifestStream(execution);
			final ListingType manifestListing = unmarshaller.unmarshal(new StreamSource(manifestStream), ListingType.class).getValue();

			String readmeFilename = null;
			if (manifestListing != null) {
				final FolderType rootFolder = manifestListing.getFolder();
				if (rootFolder != null) {
					final List<FileType> files = rootFolder.getFile();
					for (final FileType file : files) {
						final String filename = file.getName();
						if (file.getName().startsWith(README_FILENAME_PREFIX) && filename.endsWith(README_FILENAME_EXTENSION)) {
							readmeFilename = filename;
							break;
						}
					}
				}
			} else {
				LOGGER.warn("Can not generate readme, manifest listing is null.");
			}
			if (readmeFilename != null) {
				final AsyncPipedStreamBean asyncPipedStreamBean = dao.getOutputFileOutputStream(execution, readmeFilename);
				try (OutputStream readmeOutputStream = asyncPipedStreamBean.getOutputStream()) {
					Product product = execution.getProduct();
					readmeGenerator.generate(product.getReadmeHeader(), product.getReadmeEndDate(), manifestListing, readmeOutputStream);
					asyncPipedStreamBean.waitForFinish();
				}
			} else {
				LOGGER.warn("Can not generate readme, no file found in manifest root directory starting with '{}' and ending with '{}'",
						README_FILENAME_PREFIX, README_FILENAME_EXTENSION);
			}
		} catch (IOException | InterruptedException | ExecutionException | JAXBException e) {
			throw new BusinessServiceException("Failed to generate readme file.", e);
		}
	}

	public void setFileProcessingFailureMaxRetry(final Integer fileProcessingFailureMaxRetry) {
		this.fileProcessingFailureMaxRetry = fileProcessingFailureMaxRetry;
	}

}
