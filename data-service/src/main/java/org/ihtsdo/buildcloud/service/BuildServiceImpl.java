package org.ihtsdo.buildcloud.service;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.README_FILENAME_EXTENSION;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.README_FILENAME_PREFIX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;
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
import org.ihtsdo.buildcloud.service.build.transform.StreamingFileTransformation;
import org.ihtsdo.buildcloud.service.build.transform.TransformationException;
import org.ihtsdo.buildcloud.service.build.transform.TransformationFactory;
import org.ihtsdo.buildcloud.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.PostConditionException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.io.Files;

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

	@Autowired
	private RelationshipHelper relationshipHelper;

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
				if (product.getBuildConfiguration().isInputFilesFixesRequired()) {
					// Removed try/catch If this is needed and fails, then we can't go further due to blank sctids
					doInputFileFixup(build);
				}
				runPreconditionChecks(build);
				final Status newStatus = build.getStatus();
				if (newStatus != preStatus) {
					dao.updateStatus(build, newStatus);
				}
			}
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create build.", e);
		} finally {
			MDC.remove(MDC_BUILD_KEY);
		}
		return build;
	}

	private void doInputFileFixup(final Build build) throws IOException, TransformationException, NoSuchAlgorithmException, ProcessingException {
		// Due to design choices made in the terminology server, we may see input files with null SCTIDs in the
		// stated relationship file. These can be resolved as we would for the post-classified inferred relationship files
		// ie look up the previous file and if not found, try the IDGen Service using a predicted UUID
		LOGGER.debug("Performing fixup on input file prior to input file validation");
		final String buildId = build.getId();
		final TransformationFactory transformationFactory = transformationService.getTransformationFactory(build);
		
		final String statedRelationshipInputFile = relationshipHelper.getStatedRelationshipInputFile(build);

		if (statedRelationshipInputFile == null) {
			LOGGER.debug("Stated Relationship Input Delta file not present for potential fix-up.");
			return;
		}

		final InputStream statedRelationshipInputFileStream = dao.getInputFileStream(build, statedRelationshipInputFile);

		// We can't replace the file while we're reading it, so use a temp file
		final File tempDir = Files.createTempDir();
		final File tempFile = new File(tempDir, statedRelationshipInputFile);
		final FileOutputStream tempOutputStream = new FileOutputStream(tempFile);
		
		// We will not reconcile relationships with previous as that can lead to duplicate SCTIDs as triples may have historically moved
		// groups.

		final StreamingFileTransformation steamingFileTransformation = transformationFactory
				.getPreProcessFileTransformation(ComponentType.RELATIONSHIP);

		// Apply transformations
		steamingFileTransformation.transformFile(statedRelationshipInputFileStream, tempOutputStream, statedRelationshipInputFile,
				build.getBuildReport());

		// Overwrite the original file, and delete local temp copy
		dao.putInputFile(build, tempFile, false);
		tempFile.delete();
		tempDir.delete();
	}

	@Override
	public Build triggerBuild(final String releaseCenterKey, final String productKey, final String buildId, Integer failureExportMax) throws BusinessServiceException {
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
				executeBuild(build, failureExportMax);
			} catch (final BusinessServiceException | NoSuchAlgorithmException e) {
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
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildId + " for product: " + productKey);
		}

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

	private void executeBuild(final Build build, Integer failureExportMax) throws BusinessServiceException, NoSuchAlgorithmException {
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
				final String relationshipSnapshotFileFromClassifer = classifierService.generateInferredRelationshipSnapshot(build, inputFileSchemaMap);
				if (relationshipSnapshotFileFromClassifer != null) {
					generator.generateRelationshipFilesFromClassifierResult(relationshipSnapshotFileFromClassifer);
				}
			} else {
				LOGGER.info("Skipping inferred relationship creation due to product configuration.");
			}
		}

		// Generate readme file
		generateReadmeFile(build);

		File zipPackage = null;
		try {
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
					rvfResultMsg = runRVFPostConditionCheck(build, zipPackage, dao.getManifestStream(build), failureExportMax);
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
			report.add("post_validation_status", rvfStatus);
			report.add("rvf_response", rvfResultMsg);
			LOGGER.info("End of running build {}", build.getUniqueId());
		}
		finally {
			org.apache.commons.io.FileUtils.deleteQuietly(zipPackage);
		}
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

	private String runRVFPostConditionCheck(final Build build, final File zipPackage, InputStream manifestInputStream, Integer failureExportMax) throws IOException,
			PostConditionException, ConfigurationException {
		LOGGER.info("Initiating RVF post-condition check for zip file {}", zipPackage.getName());
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl)) {
			final QATestConfig qaTestConfig = build.getQaTestConfig();
			// Has the client told us where to tell the RVF to store the results? Set if not
			if (qaTestConfig.getStorageLocation() == null || qaTestConfig.getStorageLocation().length() == 0) {
				final String storageLocation = build.getProduct().getReleaseCenter().getBusinessKey() 
						+ "/" + build.getProduct().getBusinessKey()
						+ "/" + build.getId();
				qaTestConfig.setStorageLocation(storageLocation);
			}
			validateQaTestConfig(qaTestConfig, build.getConfiguration().isFirstTimeRelease());
			
			return rvfClient.checkOutputPackage(zipPackage, qaTestConfig, manifestInputStream, failureExportMax);
		}
	}

	private void validateQaTestConfig(final QATestConfig qaTestConfig , boolean isFirstTimeRelease) throws ConfigurationException {
		if (qaTestConfig == null || qaTestConfig.getAssertionGroupNames() == null) {
			throw new ConfigurationException ("No QA test configured. Please check the assertion group name is specifield.");
		}
		if (!isFirstTimeRelease) {
			if (qaTestConfig.getPreviousInternationalRelease() == null) {
				throw new ConfigurationException ("No previous international release is configured for non-first time release.");
			}
			if (qaTestConfig.getPreviousExtensionRelease() != null && qaTestConfig.getExtensionDependencyRelease() == null) {
				throw new ConfigurationException ("No extention dependency release is configured for extension testing.");
			}
			
			if (qaTestConfig.getExtensionDependencyRelease() != null && qaTestConfig.getPreviousExtensionRelease() == null) {
				throw new ConfigurationException ("Extension dependency release is specified but no previous extension release is configured for non-first time release testing.");
			}
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
