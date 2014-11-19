package org.ihtsdo.buildcloud.service;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Execution.Status;
import org.ihtsdo.buildcloud.entity.Package;
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
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
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
import java.io.*;
import java.util.*;
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
	private BuildDAO buildDAO;

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
	public Execution createExecutionFromBuild(final String buildCompositeKey)
			throws BusinessServiceException {

		final Date creationDate = new Date();
		final Build build = getBuild(buildCompositeKey);
		if (build.getEffectiveTime() == null) {
			throw new BadConfigurationException("Build effective time must be set before an execution is created.");
		}
		Execution execution = null;
		try {
			synchronized (buildCompositeKey) {
				// Do we already have an execution for that date?
				final Execution existingExecution = getExecution(build, creationDate);
				if (existingExecution != null) {
					throw new EntityAlreadyExistsException("An Execution for build " + buildCompositeKey + " already exists with execution id " + existingExecution.getId());
				}
				execution = new Execution(creationDate, build);
				// Create Build config export
				final String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);
				// save execution with config
				dao.save(execution, jsonConfig);
				MDC.put(MDC_EXECUTION_KEY, execution.getUniqueId());
				LOGGER.info("Created execution.", buildCompositeKey, execution.getId());
				// Copy all files from Build input and manifest directory to Execution input and manifest directory
				dao.copyAll(build, execution);
			}
			// Perform Pre-condition testing (loops through each package)
			final Status preStatus = execution.getStatus();
			runPreconditionChecks(execution);
			final Status newStatus = execution.getStatus();
			if (newStatus != preStatus) {
				dao.updateStatus(execution, newStatus);
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to create execution.", e);
		} finally {
			MDC.remove(MDC_EXECUTION_KEY);
		}
		return execution;
	}

	@Override
	public List<Execution> findAllDesc(final String buildCompositeKey) throws ResourceNotFoundException {
		final Build build = getBuild(buildCompositeKey);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}

		return dao.findAllDesc(build);
	}

	@Override
	public Execution find(final String buildCompositeKey, final String executionId) throws ResourceNotFoundException {
		final Build build = getBuild(buildCompositeKey);

		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}

		return dao.find(build, executionId);
	}

	@Override
	public String loadConfiguration(final String buildCompositeKey, final String executionId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		try {
			return dao.loadConfiguration(execution);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to load configuration.", e);
		}
	}

	@Override
	public List<ExecutionPackageDTO> getExecutionPackages(final String buildCompositeKey, final String executionId) throws BusinessServiceException {
		try {
			return getExecutionPackages(buildCompositeKey, executionId, null);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to retrieve execution packages.", e);
		}
	}

	@Override
	public ExecutionPackageDTO getExecutionPackage(final String buildCompositeKey, final String executionId, final String packageId) throws BusinessServiceException {
		final List<ExecutionPackageDTO> executionPackages;
		try {
			executionPackages = getExecutionPackages(buildCompositeKey, executionId, packageId);
			return !executionPackages.isEmpty() ? executionPackages.iterator().next() : null;
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to retrieve execution package.", e);
		}
	}

	@Override
	public Execution triggerBuild(final String buildCompositeKey, final String executionId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		final Build build = execution.getBuild();

		// Start the execution telemetry stream. All future logging on this thread and it's children will be captured.
		TelemetryStream.start(LOGGER, dao.getTelemetryExecutionLogFilePath(execution));
		LOGGER.info("Trigger build", buildCompositeKey, executionId);

		try {
			updateStatusWithChecks(execution, Status.BUILDING);

			// Execute each package
			ExecutionReport executionReport = execution.getExecutionReport();
			for (final Package pkg : build.getPackages()) {
				String resultStatus = "completed";
				String resultMessage = "Process completed successfully";
				try {
					ExecutionPackageBean executionPackageBean = new ExecutionPackageBean(execution, pkg);
					executePackage(executionPackageBean); // This could add entries to the execution report also
				} catch (final Exception e) {
					//Each package could fail independently, record telemetry and move on to next package
					resultStatus = "fail";
					resultMessage = "Failure while processing package " + pkg.getBusinessKey() + " due to: "
							+ e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
					LOGGER.warn(resultMessage, e);
				}
				final ExecutionPackageReport report = executionReport.getOrCreateExecutionPackgeReport(pkg);
				report.add("Progress Status", resultStatus);
				report.add("Message", resultMessage);
			}
			dao.persistReport(execution);

			updateStatusWithChecks(execution, Status.BUILT);
		} finally {
			// Finish the telemetry stream. Logging on this thread will no longer be captured.
			TelemetryStream.finish(LOGGER);
		}

		return execution;
	}

	@Override
	public void updateStatus(final String buildCompositeKey, final String executionId, final String statusString) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		final Execution.Status status = Execution.Status.valueOf(statusString);
		updateStatusWithChecks(execution, status);
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
	public InputStream getOutputFile(final String buildCompositeKey, final String executionId, final String packageId, final String outputFilePath) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.getOutputFileStream(execution, packageId, outputFilePath);
	}

	@Override
	public List<String> getExecutionPackageOutputFilePaths(final String buildCompositeKey, final String executionId, final String packageId) throws BusinessServiceException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.listOutputFilePaths(execution, packageId);
	}

	@Override
	public InputStream getInputFile(final String buildCompositeKey, final String executionId, final String packageId, final String inputFilePath) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.getInputFileStream(execution, packageId, inputFilePath);
	}

	@Override
	public List<String> getExecutionPackageInputFilePaths(final String buildCompositeKey, final String executionId, final String packageId) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.listInputFileNames(execution, packageId);
	}

	@Override
	public InputStream getLogFile(final String buildCompositeKey, final String executionId, final String packageId, final String logFileName) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.getLogFileStream(execution, packageId, logFileName);
	}

	@Override
	public List<String> getExecutionPackageLogFilePaths(final String buildCompositeKey, final String executionId, final String packageId) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.listLogFilePaths(execution, packageId);
	}

	@Override
	public List<String> getExecutionLogFilePaths(final String buildCompositeKey, final String executionId) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.listExecutionLogFilePaths(execution);
	}

	@Override
	public InputStream getExecutionLogFile(final String buildCompositeKey, final String executionId, final String logFileName) throws ResourceNotFoundException {
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		return dao.getExecutionLogFileStream(execution, logFileName);
	}

	private void runPreconditionChecks(final Execution execution) {
	    LOGGER.info("Start of Pre-condition checks");
		final Map<String, List<PreConditionCheckReport>> preConditionReports = preconditionManager.runPreconditionChecks(execution);
		execution.setPreConditionCheckReports(preConditionReports);
		//analyze report to check whether there is fatal error for all packages
		int fatalCountByPkg = 0;
		for (final String pkgName : preConditionReports.keySet()) {
			for (final PreConditionCheckReport report : preConditionReports.get(pkgName)) {
				if (report.getResult() == State.FATAL) {
					fatalCountByPkg++;
					LOGGER.warn("Fatal error occurred during pre-condition check for package {}", pkgName);
					break;
				}
			}
		}
		//need to alert release manager as all packages have fatal pre-condition check error.
		if (fatalCountByPkg > 0 && fatalCountByPkg == execution.getBuild().getPackages().size()) {
			execution.setStatus(Status.FAILED_PRE_CONDITIONS);
			LOGGER.warn("Fatal error occurred for all packages during pre-condition checks and execution {} will be halted.", execution.getId());
		}
		LOGGER.info("End of Pre-condition checks");
	}

	@SuppressWarnings("unchecked")
	private List<ExecutionPackageDTO> getExecutionPackages(final String buildCompositeKey, final String executionId, final String packageId) throws IOException, ResourceNotFoundException {
		final List<ExecutionPackageDTO> executionPackageDTOs = new ArrayList<>();
		final Execution execution = getExecutionOrThrow(buildCompositeKey, executionId);
		final Map<String, Object> stringObjectMap = dao.loadConfigurationMap(execution);
		final Map<String, Object> build = (Map<String, Object>) stringObjectMap.get("build");
		final List<Map<String, Object>> packages = (List<Map<String, Object>>) build.get("packages");
		for (final Map<String, Object> aPackage : packages) {
			final String id = (String) aPackage.get("id");
			if (packageId == null || packageId.equals(id)) {
				executionPackageDTOs.add(new ExecutionPackageDTO(id, (String) aPackage.get("name")));
			}
		}
		return executionPackageDTOs;
	}

	private void executePackage(final ExecutionPackageBean executionPackageBean) throws BusinessServiceException {
		final Execution execution = executionPackageBean.getExecution();
		final Package pkg = executionPackageBean.getPackage();

		LOGGER.info("Start executing package {}", pkg.getName());

		checkManifestPresent(executionPackageBean);

		if (pkg.isJustPackage()) {
			copyFilesForJustPackaging(executionPackageBean);
		} else {
			final Map<String, TableSchema> inputFileSchemaMap = getInputFileSchemaMap(executionPackageBean);
			transformationService.transformFiles(executionPackageBean, inputFileSchemaMap);

			// Convert Delta input files to Full, Snapshot and Delta release files
			Rf2FileExportRunner generator = new Rf2FileExportRunner(executionPackageBean, dao, uuidGenerator, fileProcessingFailureMaxRetry);
			generator.generateReleaseFiles();

			if (pkg.isCreateInferredRelationships()) {
				// Run classifier against concept and stated relationship snapshots to produce inferred relationship snapshot
				String relationshipSnapshotOutputFilename = classifierService.generateInferredRelationshipSnapshot(execution, pkg, inputFileSchemaMap);
				if (relationshipSnapshotOutputFilename != null) {
					generator.generateDeltaAndFullFromSnapshot(relationshipSnapshotOutputFilename);
				}
			} else {
				LOGGER.info("Skipping inferred relationship creation due to build configuration.");
			}
		}

		// Generate readme file
		generateReadmeFile(executionPackageBean);

		File zipPackage;
		try {
			final Zipper zipper = new Zipper(executionPackageBean, dao);
			zipPackage = zipper.createZipFile();
			LOGGER.info("Start: Upload zipPackage file {}", zipPackage.getName());
			dao.putOutputFile(execution, pkg, zipPackage, "", true);
			LOGGER.info("Finish: Upload zipPackage file {}", zipPackage.getName());
		} catch (JAXBException | IOException | ResourceNotFoundException e) {
			throw new BusinessServiceException("Failure in Zip creation caused by " + e.getMessage(), e);
		}

		String rvfStatus = "N/A";
		String rvfResultMsg = "RVF validation configured to not run.";
		if (!offlineMode || localRvf) {
			try {
				rvfResultMsg = runRVFPostConditionCheck(executionPackageBean, zipPackage);
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
		final ExecutionPackageReport report = executionPackageBean.getExecutionPackageReport();
		report.add("Post Validation Status", rvfStatus);
		report.add("RVF Test Failures", rvfResultMsg);

		LOGGER.info("End of executing package {}", pkg.getName());
	}

	private void checkManifestPresent(ExecutionPackageBean executionPackageBean) throws BusinessServiceException {
		try {
			final InputStream manifestStream = dao.getManifestStream(executionPackageBean.getExecution(), executionPackageBean.getPackage());
			if (manifestStream == null) {
				throw new BadConfigurationException("Failed to find valid manifest file.");
			} else {
				manifestStream.close();
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to close manifest file.", e);
		}
	}

	private String runRVFPostConditionCheck(final ExecutionPackageBean executionPackageBean, final File zipPackage) throws IOException,
			PostConditionException {
	    	LOGGER.info("Run RVF post-condition check for zip file {}", zipPackage.getName());
		String logFilename = "postcheck-rvf-" + zipPackage.getName() + ".log";
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl);
			 AsyncPipedStreamBean logFileOutputStream = dao.getLogFileOutputStream(
					 executionPackageBean.getExecution(),
					 executionPackageBean.getPackage().getBusinessKey(),
					 logFilename)) {
			return rvfClient.checkOutputPackage(zipPackage, logFileOutputStream);
		}
	}

	private void copyFilesForJustPackaging(final ExecutionPackageBean executionPackageBean) {
		final String packageBusinessKey = executionPackageBean.getPackage().getBusinessKey();
		final Execution execution = executionPackageBean.getExecution();
		LOGGER.info("Just copying files in execution {}, package {} for packaging", execution.getId(), packageBusinessKey);

		// Iterate each execution input file
		final List<String> executionInputFilePaths = dao.listInputFileNames(execution, packageBusinessKey);
		for (final String relativeFilePath : executionInputFilePaths) {
			dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
		}
	}

	private Map<String, TableSchema> getInputFileSchemaMap(final ExecutionPackageBean executionPackageBean) throws BusinessServiceException {
		final List<String> executionInputFilePaths = dao.listInputFileNames(executionPackageBean.getExecution(), executionPackageBean.getPackage().getBusinessKey());
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

	private Execution getExecutionOrThrow(final String buildCompositeKey, final String executionId) throws ResourceNotFoundException {
		final Execution execution = getExecution(buildCompositeKey, executionId);
		if (execution == null) {
			final String item = CompositeKeyHelper.getPath(buildCompositeKey, executionId);
			throw new ResourceNotFoundException("Unable to find execution: " + item);
		}
		return execution;
	}

	private Execution getExecution(final String buildCompositeKey, final String executionId) throws ResourceNotFoundException {
		final Build build = getBuild(buildCompositeKey);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return dao.find(build, executionId);
	}

	private Execution getExecution(final Build build, final Date creationTime) {
		return dao.find(build, EntityHelper.formatAsIsoDateTime(creationTime));
	}
	

	private Build getBuild(final String buildCompositeKey) throws ResourceNotFoundException {
		final Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return buildDAO.find(buildId, SecurityHelper.getRequiredUser());
	}

	private void generateReadmeFile(final ExecutionPackageBean executionPackageBean) throws BusinessServiceException {
		try {
			final Execution execution = executionPackageBean.getExecution();
			final Package pkg = executionPackageBean.getPackage();
			LOGGER.info("Generating readMe file for package {}", pkg.getName());
			final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
			final InputStream manifestStream = dao.getManifestStream(execution, pkg);
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
				final AsyncPipedStreamBean asyncPipedStreamBean = dao.getOutputFileOutputStream(execution, pkg.getBusinessKey(), readmeFilename);
				try (OutputStream readmeOutputStream = asyncPipedStreamBean.getOutputStream()) {
					readmeGenerator.generate(pkg.getReadmeHeader(), pkg.getReadmeEndDate(), manifestListing, readmeOutputStream);
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
