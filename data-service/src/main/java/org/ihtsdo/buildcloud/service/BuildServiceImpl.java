package org.ihtsdo.buildcloud.service;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.README_FILENAME_EXTENSION;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.README_FILENAME_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Build.Status;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.BuildReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.Rf2FileExportRunner;
import org.ihtsdo.buildcloud.service.build.Zipper;
import org.ihtsdo.buildcloud.service.build.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.PostConditionException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BuildServiceImpl implements BuildService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImpl.class);

	@Autowired
	private BuildDAO dao;

	@Autowired
	private ProductDAO productDAO;

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
	public Build createBuildFromProduct(final String releaseCenterKey, final String productKey) throws BusinessServiceException {
		final Date creationDate = new Date();
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product.getBuildConfiguration().getEffectiveTime() == null) {
			throw new BadConfigurationException("Product effective time must be set before an build is created.");
		}
		Build build;
		try {
			synchronized (product) {
				// Do we already have an build for that date?
				final Build existingBuild = getBuild(product, creationDate);
				if (existingBuild != null) {
					throw new EntityAlreadyExistsException("An Build for product " + productKey + " already exists with build id " + existingBuild.getId());
				}
				build = new Build(creationDate, product);
				build.setProduct(product);
				build.setQaTestConfig(product.getQaTestConfig());
				// save build with config
				MDC.put(MDC_BUILD_KEY, build.getUniqueId());
				dao.save(build);
				LOGGER.info("Created build.", productKey, build.getId());
				// Copy all files from Product input and manifest directory to Build input and manifest directory
				dao.copyAll(product, build);
			}
			if (!product.getBuildConfiguration().isJustPackage()) {
				// Perform Pre-condition testing
				final Status preStatus = build.getStatus();
				runPreconditionChecks(build);
				final Status newStatus = build.getStatus();
				if (newStatus != preStatus) {
					dao.updateStatus(build, newStatus);
				}
			}
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to create build.", e);
		} finally {
			MDC.remove(MDC_BUILD_KEY);
		}
		return build;
	}

	@Override
	public Build triggerBuild(final String releaseCenterKey, final String productKey, final String buildId) throws BusinessServiceException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		try {
			dao.loadConfiguration(build);
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to load build configuration.", e);
		}

		// Start the build telemetry stream. All future logging on this thread and it's children will be captured.
		TelemetryStream.start(LOGGER, dao.getTelemetryBuildLogFilePath(build));
		LOGGER.info("Trigger product", productKey, buildId);

		try {
			updateStatusWithChecks(build, Status.BUILDING);

			// Run product
			final BuildReport report = build.getBuildReport();
			String resultStatus = "completed";
			String resultMessage = "Process completed successfully";
			try {
				executeBuild(build);
			} catch (final BusinessServiceException e) {
				resultStatus = "fail";
				resultMessage = "Failure while processing build " + build.getUniqueId() + " due to: "
						+ e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
				LOGGER.warn(resultMessage, e);
			}
			report.add("Progress Status", resultStatus);
			report.add("Message", resultMessage);
			dao.persistReport(build); // TODO: Does this work?

			updateStatusWithChecks(build, Status.BUILT);
		} finally {
			// Finish the telemetry stream. Logging on this thread will no longer be captured.
			TelemetryStream.finish(LOGGER);
		}

		return build;
	}

	@Override
	public List<Build> findAllDesc(final String releaseCenterKey, final String productKey) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}

		return dao.findAllDesc(product);
	}

	@Override
	public Build find(final String releaseCenterKey, final String productKey, final String buildId) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}

		final Build build = dao.find(product, buildId);
		build.setProduct(product);
		return build;
	}

	@Override
	public BuildConfiguration loadBuildConfiguration(final String releaseCenterKey, final String productKey, final String buildId) throws BusinessServiceException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		try {
			dao.loadBuildConfiguration(build);
			return build.getConfiguration();
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to load configuration.", e);
		}
	}

	private void updateStatusWithChecks(final Build build, final Status newStatus) throws BadConfigurationException {
		// Assert status workflow position
		switch (newStatus) {
			case BUILDING :
				dao.assertStatus(build, Status.BEFORE_TRIGGER);
				break;
			case BUILT :
				dao.assertStatus(build, Status.BUILDING);
				break;
		}

		dao.updateStatus(build, newStatus);
	}

	@Override
	public InputStream getOutputFile(final String releaseCenterKey, final String productKey, final String buildId, final String outputFilePath) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getOutputFileStream(build, outputFilePath);
	}

	@Override
	public List<String> getOutputFilePaths(final String releaseCenterKey, final String productKey, final String buildId) throws BusinessServiceException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.listOutputFilePaths(build);
	}

	@Override
	public InputStream getInputFile(final String releaseCenterKey, final String productKey, final String buildId, final String inputFilePath) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getInputFileStream(build, inputFilePath);
	}

	@Override
	public List<String> getInputFilePaths(final String releaseCenterKey, final String productKey, final String buildId) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.listInputFileNames(build);
	}

	@Override
	public InputStream getLogFile(final String releaseCenterKey, final String productKey, final String buildId, final String logFileName) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getLogFileStream(build, logFileName);
	}

	@Override
	public List<String> getLogFilePaths(final String releaseCenterKey, final String productKey, final String buildId) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.listBuildLogFilePaths(build);
	}

	private void runPreconditionChecks(final Build build) {
	    LOGGER.info("Start of Pre-condition checks");
		final List<PreConditionCheckReport> preConditionReports = preconditionManager.runPreconditionChecks(build);
		build.setPreConditionCheckReports(preConditionReports);
		// analyze report to check whether there is fatal error for all packages
		for (final PreConditionCheckReport report : preConditionReports) {
			if (report.getResult() == State.FATAL) {
				// Need to alert release manager of fatal pre-condition check error.
				build.setStatus(Status.FAILED_PRE_CONDITIONS);
				LOGGER.warn("Fatal error occurred during pre-condition checks, build {} will be halted.", build.getId());
				break;
			}
		}
		LOGGER.info("End of Pre-condition checks");
	}

	private void executeBuild(final Build build) throws BusinessServiceException {
		LOGGER.info("Start build {}", build.getUniqueId());
		checkManifestPresent(build);

		final BuildConfiguration configuration = build.getConfiguration();
		if (configuration.isJustPackage()) {
			copyFilesForJustPackaging(build);
		} else {
			final Map<String, TableSchema> inputFileSchemaMap = getInputFileSchemaMap(build);
			transformationService.transformFiles(build, inputFileSchemaMap);

			// Convert Delta input files to Full, Snapshot and Delta release files
			final Rf2FileExportRunner generator = new Rf2FileExportRunner(build, dao, uuidGenerator, fileProcessingFailureMaxRetry);
			generator.generateReleaseFiles();

			if (configuration.isCreateInferredRelationships()) {
				// Run classifier against concept and stated relationship snapshots to produce inferred relationship snapshot
				final String relationshipSnapshotOutputFilename = classifierService.generateInferredRelationshipSnapshot(build, inputFileSchemaMap);
				if (relationshipSnapshotOutputFilename != null) {
					generator.generateDeltaAndFullFromSnapshot(relationshipSnapshotOutputFilename);
				}
			} else {
				LOGGER.info("Skipping inferred relationship creation due to product configuration.");
			}
		}

		// Generate readme file
		generateReadmeFile(build);

		File zipPackage;
		try {
			final Zipper zipper = new Zipper(build, dao);
			zipPackage = zipper.createZipFile();
			LOGGER.info("Start: Upload zipPackage file {}", zipPackage.getName());
			dao.putOutputFile(build, zipPackage, true);
			LOGGER.info("Finish: Upload zipPackage file {}", zipPackage.getName());
		} catch (JAXBException | IOException | ResourceNotFoundException e) {
			throw new BusinessServiceException("Failure in Zip creation caused by " + e.getMessage(), e);
		}

		String rvfStatus = "N/A";
		String rvfResultMsg = "RVF validation configured to not run.";
		if (!offlineMode || localRvf) {
			try {
				rvfResultMsg = runRVFPostConditionCheck(build, zipPackage);
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
		final BuildReport report = build.getBuildReport();
		report.add("Post Validation Status", rvfStatus);
		report.add("RVF Test Failures", rvfResultMsg);

		LOGGER.info("End of running build {}", build.getUniqueId());
	}

	private void checkManifestPresent(final Build build) throws BusinessServiceException {
		try {
			final InputStream manifestStream = dao.getManifestStream(build);
			if (manifestStream == null) {
				throw new BadConfigurationException("Failed to find valid manifest file.");
			} else {
				manifestStream.close();
			}
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to close manifest file.", e);
		}
	}

	private String runRVFPostConditionCheck(final Build build, final File zipPackage) throws IOException,
			PostConditionException {
		LOGGER.info("Run RVF post-condition check for zip file {}", zipPackage.getName());
		final String logFilename = "postcheck-rvf-" + zipPackage.getName() + ".log";
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl);
			 AsyncPipedStreamBean logFileOutputStream = dao.getLogFileOutputStream(build, logFilename)) {
			LOGGER.info("RVF Log file established: ", logFilename);
			final QATestConfig qaTestConfig = build.getQaTestConfig();
			if (qaTestConfig != null) {
				return rvfClient.checkOutputPackage(zipPackage, logFileOutputStream, qaTestConfig);
			}
			return rvfClient.checkOutputPackage(zipPackage, logFileOutputStream);
		}
	}

	private void copyFilesForJustPackaging(final Build build) {
		LOGGER.info("Just copying files in build {} for packaging", build.getUniqueId());

		// Iterate each build input file
		final List<String> buildInputFilePaths = dao.listInputFileNames(build);
		for (final String relativeFilePath : buildInputFilePaths) {
			dao.copyInputFileToOutputFile(build, relativeFilePath);
		}
	}

	private Map<String, TableSchema> getInputFileSchemaMap(final Build build) throws BusinessServiceException {
		final List<String> buildInputFilePaths = dao.listInputFileNames(build);
		final Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		for (final String buildInputFilePath : buildInputFilePaths) {
			final TableSchema schemaBean;
			try {
				schemaBean = schemaFactory.createSchemaBean(FileUtils.getFilenameFromPath(buildInputFilePath));
			} catch (final FileRecognitionException e) {
				throw new BusinessServiceException("Did not recognise input file '" + buildInputFilePath + "'", e);
			}
			inputFileSchemaMap.put(buildInputFilePath, schemaBean);
		}
		return inputFileSchemaMap;
	}

	private Build getBuildOrThrow(final String releaseCenterKey, final String productKey, final String buildId) throws ResourceNotFoundException {
		final Build build = find(releaseCenterKey, productKey, buildId);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build for releaseCenterKey: " + releaseCenterKey + ", productKey: " + productKey + ", buildId: " + buildId);
		}
		return build;
	}

	private Build getBuild(final Product product, final Date creationTime) {
		return dao.find(product, EntityHelper.formatAsIsoDateTime(creationTime));
	}

	private Product getProduct(final String releaseCenterKey, final String productKey) throws ResourceNotFoundException {
		return productDAO.find(releaseCenterKey, productKey, SecurityHelper.getRequiredUser());
	}

	private void generateReadmeFile(final Build build) throws BusinessServiceException {
		try {
			LOGGER.info("Generating readMe file for build {}", build.getUniqueId());
			final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
			final InputStream manifestStream = dao.getManifestStream(build);
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
				final AsyncPipedStreamBean asyncPipedStreamBean = dao.getOutputFileOutputStream(build, readmeFilename);
				try (OutputStream readmeOutputStream = asyncPipedStreamBean.getOutputStream()) {
					final BuildConfiguration configuration = build.getConfiguration();
					readmeGenerator.generate(configuration.getReadmeHeader(), configuration.getReadmeEndDate(), manifestListing, readmeOutputStream);
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

	@Override
	public QATestConfig loadQATestConfig(final String releaseCenterKey, final String productKey, final String buildId) throws BusinessServiceException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		try {
			dao.loadBuildConfiguration(build);
			return build.getQaTestConfig();
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to load configuration.", e);
		}
	}

}
