package org.ihtsdo.buildcloud.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.config.DailyBuildResourceConfig;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.entity.Build.Status;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.manifest.*;
import org.ihtsdo.buildcloud.core.releaseinformation.ConceptMini;
import org.ihtsdo.buildcloud.core.service.build.*;
import org.ihtsdo.buildcloud.core.service.build.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.StreamingFileTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.TransformationException;
import org.ihtsdo.buildcloud.core.service.build.transform.TransformationFactory;
import org.ihtsdo.buildcloud.core.service.build.transform.TransformationService;
import org.ihtsdo.buildcloud.core.service.helper.ManifestXmlFileParser;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.core.service.validation.postcondition.PostconditionManager;
import org.ihtsdo.buildcloud.core.service.validation.precondition.ManifestFileListingHelper;
import org.ihtsdo.buildcloud.core.service.validation.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.core.service.validation.rvf.RVFClient;
import org.ihtsdo.buildcloud.core.service.validation.rvf.ValidationRequest;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.buildcloud.rest.pojo.BuildRequestPojo;
import org.ihtsdo.buildcloud.telemetry.client.TelemetryStream;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.resourcemanager.ResourceManager;
import org.ihtsdo.otf.rest.exception.*;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import javax.naming.ConfigurationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildServiceImpl implements BuildService {
	private static final String HYPHEN = "-";

	private static final String ADDITIONAL_RELATIONSHIP = "900000000000227009";

	private static final String STATED_RELATIONSHIP = "_StatedRelationship_";

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildServiceImpl.class);
	public static final String UNABLE_TO_FIND_PRODUCT = "Unable to find product: ";
	public static final String PROGRESS_STATUS = "Progress Status";
	public static final String MESSAGE = "Message";

	@Autowired
	private BuildDAO dao;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private PreconditionManager preconditionManager;

	@Autowired
	private PostconditionManager postconditionManager;

	@Autowired
	private ReadmeGenerator readmeGenerator;

	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private TransformationService transformationService;

	@Autowired
	private PublishService publishService;

	@Autowired
	private S3PathHelper pathHelper;

	@Value("${srs.file-processing.failureMaxRetry}")
	private Integer fileProcessingFailureMaxRetry;

	@Value("${rvf.url}")
	private String releaseValidationFrameworkUrl;

	@Value("${srs.build.offlineMode}")
	private Boolean offlineMode;

	@Value("${srs.storage.bucketName}")
	private String buildBucketName;

	@Value("${srs.jms.queue.prefix}.build-job-status")
	private String queue;

	@Value("${srs.jms.queue.prefix}.daily-build-rvf-response-queue")
	private String dailyBuildRvfResponseQueue;

	@Autowired
	private DailyBuildResourceConfig dailyBuildResourceConfig;

	@Autowired
	private ResourceLoader cloudResourceLoader;

	private ResourceManager dailyBuildResourceManager;

	@Autowired
	private MessagingHelper messagingHelper;

	@Autowired
	private ActiveMQTextMessage buildStatusTextMessage;

	@Autowired
	private BuildStatusTrackerDao statusTrackerDao;

	@Autowired
	private InputFileService inputFileService;

	@PostConstruct
	public void init() {
		dailyBuildResourceManager = new ResourceManager(dailyBuildResourceConfig, cloudResourceLoader);
	}

	@Override
	public Build createBuildFromProduct(String releaseCenterKey, String productKey, BuildRequestPojo buildRequest, String user, List<String> userRoles) throws BusinessServiceException {
		final Date creationDate = new Date();
		final Product product = getProduct(releaseCenterKey, productKey);

		if (!product.isVisibility()) {
			throw new BusinessServiceException("Could not create build from invisible product with key: " + product.getBusinessKey());
		}

		Date effectiveTime = null;
		if (buildRequest != null) {
			try {
				effectiveTime = RF2Constants.DATE_FORMAT.parse(buildRequest.getEffectiveDate());
			} catch (ParseException e) {
				throw new BusinessServiceRuntimeException("Could not parse effectiveDate.");
			}
		}

		validateBuildConfig(product.getBuildConfiguration(), effectiveTime, buildRequest.getBranchPath());
		Build build;
		try {
			// Do we already have an build for that date?
			final Build existingBuild = getBuild(releaseCenterKey, productKey, creationDate);
			if (existingBuild != null) {
				throw new EntityAlreadyExistsException("An Build for product " + productKey + " already exists with build id " + existingBuild.getId());
			}
			build = new Build(creationDate, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
			ObjectMapper objectMapper = new ObjectMapper();
			if (build.getConfiguration() != null) {
				BuildConfiguration configuration = objectMapper.readValue(objectMapper.writeValueAsString(build.getConfiguration()), BuildConfiguration.class);
				if (effectiveTime != null) {
					configuration.setEffectiveTime(effectiveTime);
				}
				configuration.setBuildPackageName(getBuildPackageName(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey()));
				configuration.setStandAloneProduct(product.isStandAloneProduct());
				if (buildRequest != null) {
					configuration.setBranchPath(buildRequest.getBranchPath());
					configuration.setExportType(buildRequest.getExportCategory() != null ? buildRequest.getExportCategory().name() : null);
					configuration.setBuildName(buildRequest.getBuildName());
					configuration.setLoadExternalRefsetData(buildRequest.isLoadExternalRefsetData());
					configuration.setLoadTermServerData(buildRequest.isLoadTermServerData());
					configuration.setReplaceExistingEffectiveTime(buildRequest.isReplaceExistingEffectiveTime());
				}

				build.setConfiguration(configuration);
			}
			build.setQaTestConfig(product.getQaTestConfig());
			if (build.getQaTestConfig() != null) {
				QATestConfig qaTestConfig = objectMapper.readValue(objectMapper.writeValueAsString(build.getQaTestConfig()), QATestConfig.class);
				if (buildRequest != null) {
					qaTestConfig.setMaxFailureExport(buildRequest.getMaxFailuresExport() != null ? buildRequest.getMaxFailuresExport() : 100);
					qaTestConfig.setEnableTraceabilityValidation(buildRequest.isEnableTraceabilityValidation());
				} else {
					qaTestConfig.setMaxFailureExport(100);
				}

				build.setQaTestConfig(qaTestConfig);
			}
			build.setBuildUser(user);
			build.setUserRoles(userRoles);

			// create build status tracker
			BuildStatusTracker tracker = new BuildStatusTracker();
			tracker.setProductKey(build.getProductKey());
			tracker.setReleaseCenterKey(build.getReleaseCenterKey());
			tracker.setBuildId(build.getId());
			statusTrackerDao.save(tracker);

			// save build with config
			MDC.put(MDC_BUILD_KEY, build.getUniqueId());
			dao.save(build);

			// copy manifest.xml
			dao.copyManifestFileFromProduct(build);
			LOGGER.info("Release build {} created for product {}", build.getId(), productKey);
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create build for product " + productKey, e);
		} finally {
			MDC.remove(MDC_BUILD_KEY);
		}
		return build;
	}

	private void validateBuildConfig(BuildConfiguration buildConfiguration, Date buildEffectiveTime, String branchPath) throws BadConfigurationException {
		if (buildEffectiveTime == null && buildConfiguration.getEffectiveTime() == null) {
			throw new BadConfigurationException("The effective time must be set before a build is created.");
		}
		ExtensionConfig extensionConfig = buildConfiguration.getExtensionConfig();
		if (extensionConfig != null) {
			if (extensionConfig.getModuleIdsSet() == null || extensionConfig.getModuleIdsSet().isEmpty()) {
				throw new BadConfigurationException("The module ids must be set for " + (!StringUtils.hasLength(branchPath) ? "a derivative product." : "an extension build."));
			}
			if (extensionConfig.getNamespaceId() == null || extensionConfig.getNamespaceId().isEmpty()) {
				throw new BadConfigurationException("The namespace must be set for " + (!StringUtils.hasLength(branchPath) ? "a derivative product." : "an extension build."));
			}
		}
	}

	private void doInputFileFixup(final Build build) throws BusinessServiceException {
		// Due to design choices made in the terminology server, we may see input files with null SCTIDs in the
		// stated relationship file. These can be resolved as we would for the post-classified inferred relationship files
		// ie look up the previous file and if not found, try the IDGen Service using a predicted UUID
		File tempDir = null;
		File tempFile = null;
		try {
			LOGGER.debug("Performing fixup on input file prior to input file validation");
			final TransformationFactory transformationFactory = transformationService.getTransformationFactory(build);
			final String statedRelationshipInputFile = getStatedRelationshipInputFile(build);
			if (statedRelationshipInputFile == null) {
				LOGGER.debug("Stated Relationship Input Delta file not present for potential fix-up.");
				return;
			}
			InputStream statedRelationshipInputFileStream = dao.getInputFileStream(build, statedRelationshipInputFile);

			// We can't replace the file while we're reading it, so use a temp file
			tempDir = Files.createTempDirectory("tmp").toFile();
			tempFile = new File(tempDir, statedRelationshipInputFile);
			try (final FileOutputStream tempOutputStream = new FileOutputStream(tempFile)) {
				// We will not reconcile relationships with previous as that can lead to duplicate SCTIDs as triples may have historically moved
				// groups.

				final StreamingFileTransformation steamingFileTransformation = transformationFactory
						.getPreProcessFileTransformation(ComponentType.RELATIONSHIP);

				// Apply transformations
				steamingFileTransformation.transformFile(statedRelationshipInputFileStream, tempOutputStream, statedRelationshipInputFile,
						build.getBuildReport());

				// Overwrite the original file, and delete local temp copy
				dao.putInputFile(build, tempFile, false);
			}
		} catch (IOException | NoSuchAlgorithmException | TransformationException e) {
			String msg = String.format("Error while doing input file fix up. Message: %s", e.getMessage());
			throw new BusinessServiceException(msg, e);
		} finally {
			if (tempFile != null) {
				tempFile.delete();
			}
			if (tempDir != null) {
				tempDir.delete();
			}
		}
	}

	private String getStatedRelationshipInputFile(Build build) {
		//get a list of input file names
		final List<String> inputFilenames = dao.listInputFileNames(build);
		for (final String inputFileName : inputFilenames) {
			if (inputFileName.contains(STATED_RELATIONSHIP)) {
				return inputFileName;
			}
		}
		return null;
	}

	private String getBuildPackageName(String releaseCenter, String productKey) throws IOException, JAXBException {
		ListingType manifestListing;
		try (InputStream manifestInputStream = inputFileService.getManifestStream(releaseCenter, productKey)) {
			if (manifestInputStream == null) {
				return null;
			}
			ManifestXmlFileParser parser = new ManifestXmlFileParser();
			manifestListing = parser.parse(manifestInputStream);
		}
		// Zip file name is the same as the root folder defined in manifest, with .zip appended
		FolderType rootFolder = manifestListing.getFolder();
		return rootFolder.getName() + ".zip";
	}

	@Override
	public Build triggerBuild(Build build, Boolean enableTelemetryStream) throws IOException {
		if (dao.isBuildCancelRequested(build)) return build;

		// Start the build telemetry stream. All future logging on this thread and it's children will be captured.
		try {
			if (Boolean.TRUE.equals(enableTelemetryStream)) {
				TelemetryStream.start(LOGGER, dao.getTelemetryBuildLogFilePath(build));
			}
			// Get build configurations from S3
			getBuildConfigurations(build);
			if (dao.isBuildCancelRequested(build)) return build;

			performPreconditionTesting(build);
			// Stop processing when the pre_condition checks have failed
			if (Status.FAILED_PRE_CONDITIONS == build.getStatus()) {
				return build;
			}
			if (dao.isBuildCancelRequested(build)) return build;

			boolean isAbandoned = checkSourceFile(build);
			// execute build
			if (!isAbandoned) {
				Status status = Status.BUILDING;
				if (Boolean.TRUE.equals(offlineMode)) {
					performOfflineBuild(build, status);
				} else {
					updateStatusWithChecks(build, status);
					executeBuild(build);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error occurred while trying to trigger the build {}.", build.getId(), e);
			dao.updateStatus(build, Status.FAILED);
		} finally {
			// Finish the telemetry stream. Logging on this thread will no longer be captured.
			if (Boolean.TRUE.equals(enableTelemetryStream)) {
				TelemetryStream.finish(LOGGER);
			}
		}
		return build;
	}

	private void performOfflineBuild(Build build, Status status) throws BadConfigurationException, IOException {
		final BuildReport report = build.getBuildReport();
		String resultStatus = "completed";
		String resultMessage = "Process completed successfully";
		try {
			updateStatusWithChecks(build, status);
			executeBuild(build);

			// Check warnings if any
			boolean hasWarnings = false;
			if (build.getPreConditionCheckReports() != null) {
				for (PreConditionCheckReport conditionCheckReport : build.getPreConditionCheckReports()) {
					if (conditionCheckReport.getResult() == State.WARNING) {
						hasWarnings = true;
						break;
					}
				}
			}
			status = hasWarnings ? Status.RELEASE_COMPLETE_WITH_WARNINGS : Status.RELEASE_COMPLETE;
		} catch (final Exception e) {
			resultStatus = "fail";
			resultMessage = "Failure while processing build " + build.getUniqueId() + " due to: " + e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
			LOGGER.error(resultMessage, e);
			status = Status.FAILED;
		}
		setReportStatusAndPersist(build, status, report, resultStatus, resultMessage);
	}

	private void performPreconditionTesting(final Build build) throws BusinessServiceException, IOException {
		if (!build.getConfiguration().isJustPackage()) {
			final Status preStatus = build.getStatus();
			if (build.getConfiguration().isInputFilesFixesRequired()) {
				doInputFileFixup(build);
			}
			performPreConditionsCheck(build, preStatus);
		}
	}

	private boolean checkSourceFile(final Build build) throws BadConfigurationException, IOException {
		boolean isAbandoned = false;
		try (InputStream reportStream = dao.getBuildInputFilesPrepareReportStream(build)) {
			//check source file prepare report
			if (reportStream != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				SourceFileProcessingReport sourceFilePrepareReport = objectMapper.readValue(reportStream, SourceFileProcessingReport.class);
				if (sourceFilePrepareReport.getDetails() != null && sourceFilePrepareReport.getDetails().containsKey(ReportType.ERROR)) {
					isAbandoned = true;
					updateStatusWithChecks(build, Status.FAILED_INPUT_PREPARE_REPORT_VALIDATION);
					LOGGER.error("Errors found in the source file prepare report therefore the build is abandoned. "
							+ "Please see detailed failures via the inputPrepareReport_url link listed.");
				}
			} else {
				LOGGER.warn("No source file prepare report found.");
			}
		} catch (IOException e) {
			updateStatusWithChecks(build, Status.FAILED_PRE_CONDITIONS);
			LOGGER.error("Failed to read source file processing report", e);
			isAbandoned = true;
		}
		return isAbandoned;
	}

	private void executeBuild(final Build build, final boolean isAbandoned) throws BadConfigurationException, IOException {
		if (!isAbandoned) {
			Status status = Status.BUILDING;
			String resultStatus = "completed";
			String resultMessage = "Process completed successfully";
			try {
				updateStatusWithChecks(build, status);
				executeBuild(build);
				status = Status.RELEASE_COMPLETE;
			} catch (final Exception e) {
				resultStatus = "fail";
				resultMessage = "Failure while processing build " + build.getUniqueId() + " due to: "
						+ e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
				LOGGER.error(resultMessage, e);
				status = Status.FAILED;
			}
			setReportStatusAndPersist(build, status, build.getBuildReport(), resultStatus, resultMessage);
		}
	}

	public void setReportStatusAndPersist(final Build build, final Status status, final BuildReport report, final String resultStatus,
			final String resultMessage) throws BadConfigurationException, IOException {
		report.add(PROGRESS_STATUS, resultStatus);
		report.add(MESSAGE, resultMessage);
		dao.persistReport(build);
		updateStatusWithChecks(build, status);
	}

	private void performPreConditionsCheck(Build build, Status preStatus) throws BusinessServiceException, IOException {
		final Status newStatus = runPreconditionChecks(build);
		try {
			dao.updatePreConditionCheckReport(build);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to update Pre condition Check Report.", e);
		}
		if (newStatus != preStatus) {
			dao.updateStatus(build, newStatus);
		}
	}

	private void performPostConditionsCheck(Build build, Status preStatus) throws BusinessServiceException, IOException {
		final Status newStatus = runPostconditionChecks(build);
		if (newStatus != preStatus) {
			dao.updateStatus(build, newStatus);
		}
	}

	@Override
	public List<Build> findAllDesc(final String releaseCenterKey, final String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean useVisibilityFlag) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT + productKey);
		}
		return dao.findAllDesc(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, useVisibilityFlag);
	}

	@Override
	public BuildPage<Build> findAll(final String releaseCenterKey, final String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean useVisibilityFlag, View viewMode, List<Integer> forYears, PageRequest pageRequest) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT + productKey);
		}
		return dao.findAll(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, useVisibilityFlag, viewMode, forYears, pageRequest);
	}

	@Override
	public Build find(final String releaseCenterKey, final String productKey, final String buildId, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean useVisibilityFlag) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT + productKey);
		}

		final Build build = dao.find(releaseCenterKey, productKey, buildId, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, useVisibilityFlag);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildId + " for product: " + productKey);
		}

		return build;
	}

	@Override
	public void markBuildAsDeleted(Build build) throws IOException {
		dao.markBuildAsDeleted(build);
	}

	@Override
	public void delete(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException {
		final Product product = getProduct(releaseCenterKey, productKey);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT + productKey);
		}
		dao.delete(releaseCenterKey, productKey, buildId);
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

	public void updateStatusWithChecks(final Build build, final Status newStatus) throws BadConfigurationException, IOException {
		// Assert status workflow position
        switch (newStatus) {
            case BUILDING -> dao.assertStatus(build, Status.BEFORE_TRIGGER);
            case BUILT -> dao.assertStatus(build, Status.BUILDING);
            case RELEASE_COMPLETE_WITH_WARNINGS, RELEASE_COMPLETE ->
                    dao.assertStatus(build, Boolean.TRUE.equals(offlineMode) ? Status.BUILDING : Status.RVF_RUNNING);
            case CANCELLED -> dao.assertStatus(build, Status.CANCEL_REQUESTED);
        }

		dao.updateStatus(build, newStatus);
	}

	@Override
	public InputStream getOutputFile(final String releaseCenterKey, final String productKey, final String buildId, final String outputFilePath) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getOutputFileStream(build, outputFilePath);
	}

	@Override
	public List<String> getOutputFilePaths(final String releaseCenterKey, final String productKey, final String buildId) {
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

	private Build.Status runPreconditionChecks(final Build build) {
		LOGGER.info("Start of Pre-condition checks");
		Build.Status buildStatus = build.getStatus();
		final List<PreConditionCheckReport> preConditionReports = preconditionManager.runPreconditionChecks(build);
		build.setPreConditionCheckReports(preConditionReports);
		// analyze report to check whether there is fatal error for all packages
		for (final PreConditionCheckReport report : preConditionReports) {
			if (report.getResult() == State.FATAL || report.getResult() == State.FAIL) {
				// Need to alert release manager of fatal pre-condition check error.
				buildStatus = Status.FAILED_PRE_CONDITIONS;
				LOGGER.error("Fatal error occurred during pre-condition checks:{}, build {} will be halted.", report, build.getId());
				break;
			}
		}
		LOGGER.info("End of Pre-condition checks");
		return buildStatus;
	}

	private Build.Status runPostconditionChecks(final Build build) throws BusinessServiceException {
		LOGGER.info("Start of Post-condition checks");
		Build.Status buildStatus = build.getStatus();
		final List<PostConditionCheckReport> reports = postconditionManager.runPostconditionChecks(build);
		try {
			dao.updatePostConditionCheckReport(build, reports);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to update Post condition Check Report", e);
		}

		// analyze report to check whether there is fatal error for all packages
		for (final PostConditionCheckReport report : reports) {
			if (report.getResult() == PostConditionCheckReport.State.FATAL
			 || report.getResult() == PostConditionCheckReport.State.FAILED) {
				// Need to alert release manager of fatal post-condition check error.
				buildStatus = Status.FAILED_POST_CONDITIONS;
				break;
			}
		}
		LOGGER.info("End of Post-condition checks");
		return buildStatus;
	}

	private void getBuildConfigurations(Build build) throws BusinessServiceException {
		try {
			dao.loadConfiguration(build);
		} catch (final IOException e) {
			throw new BusinessServiceException(String.format("Failed to load configuration for build %s", build.getId()), e);
		}
	}

	private void executeBuild(final Build build) throws BusinessServiceException, NoSuchAlgorithmException, IOException {
		LOGGER.info("Start build {}", build.getUniqueId());
		checkManifestPresent(build);

		final BuildConfiguration configuration = build.getConfiguration();
		if (dao.isBuildCancelRequested(build)) return;
		if (configuration.isJustPackage()) {
			copyFilesForJustPackaging(build);
		} else {
			final Map<String, TableSchema> inputFileSchemaMap = getInputFileSchemaMap(build);
			if (dao.isBuildCancelRequested(build)) return;
			transformationService.transformFiles(build, inputFileSchemaMap);
			// Convert Delta input files to Full, Snapshot and Delta release files
			if (dao.isBuildCancelRequested(build)) return;

			final Rf2FileExportRunner generator = new Rf2FileExportRunner(build, dao, fileProcessingFailureMaxRetry);

			if (!generator.isInferredRelationshipFileExist(rf2DeltaFilesSpecifiedByManifest(build))) {
				throw new BusinessServiceException("There is no inferred relationship delta file");
			}
			generator.generateReleaseFiles();

			//filter out additional relationships from the transformed delta
			if (dao.isBuildCancelRequested(build)) return;
			String inferredDelta = getInferredDeltaFromInput(inputFileSchemaMap);
			if (inferredDelta != null) {
				String transformedDelta = inferredDelta.replace(RF2Constants.INPUT_FILE_PREFIX, RF2Constants.SCT2);
				transformedDelta = configuration.isBetaRelease() ? BuildConfiguration.BETA_PREFIX + transformedDelta : transformedDelta;
				retrieveAdditionalRelationshipsInputDelta(build, transformedDelta);
			}
		}
		if (dao.isBuildCancelRequested(build)) return;

//		if (Boolean.FALSE.equals(offlineMode)) {
//			LOGGER.info("Start classification cross check");
//			List<PostConditionCheckReport> reports = postconditionManager.runPostconditionChecks(build);
//			dao.updatePostConditionCheckReport(build, reports);
//		}

		// Generate release package information
		String releaseFilename = getReleaseFilename(build);
		if (StringUtils.hasLength(releaseFilename) && (StringUtils.hasLength(configuration.getReleaseInformationFields())
													|| StringUtils.hasLength(configuration.getAdditionalReleaseInformationFields()))) {
			generateReleasePackageFile(build, releaseFilename);
		}
		// Generate readme file
		generateReadmeFile(build);

		if (dao.isBuildCancelRequested(build)) return;

		// create the final package
		File zipPackage = null;
		String packageName;
		try {
			final Zipper zipper = new Zipper(build, dao);
			zipPackage = zipper.createZipFile(Zipper.FileTypeOption.NONE);
			packageName = zipPackage.getName();
			LOGGER.info("Start: Upload zipPackage file {}", zipPackage.getName());
			dao.putOutputFile(build, zipPackage, true);
			LOGGER.info("Finish: Upload zipPackage file {}", zipPackage.getName());
			if (build.getConfiguration().isDailyBuild()) {
				DailyBuildRF2DeltaExtractor deltaExtractor = new DailyBuildRF2DeltaExtractor(build, dao);
				deltaExtractor.outputDailyBuildPackage(dailyBuildResourceManager);

				DailyBuildRF2SnapshotExtractor snapshotExtractor = new DailyBuildRF2SnapshotExtractor(build, dao);
				snapshotExtractor.outputDailyBuildPackage(dailyBuildResourceManager);
			}
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create zip file", e);
		} finally {
			org.apache.commons.io.FileUtils.deleteQuietly(zipPackage);
		}

		if (dao.isBuildCancelRequested(build)) return;

		if (Boolean.FALSE.equals(offlineMode)) {
			performPostConditionsCheck(build, build.getStatus());
			if (Status.FAILED_POST_CONDITIONS.equals(build.getStatus())) {
				return;
			}
			// all good so far
			dao.updateStatus(build, Status.BUILT);
		}

		if (dao.isBuildCancelRequested(build)) return;

		// run rvf validations
		String rvfStatus = "N/A";
		String rvfResultMsg = "RVF validation configured to not run.";
		if (Boolean.FALSE.equals(offlineMode)) {
			String s3ZipFilePath = dao.getOutputFilePath(build, packageName);
			final QATestConfig qaTestConfig = build.getQaTestConfig();
			rvfResultMsg = runRVFPostConditionCheck(build, s3ZipFilePath, dao.getManifestFilePath(build), qaTestConfig.getMaxFailureExport());
			if (rvfResultMsg == null) {
				rvfStatus = "Failed to run";
			} else {
				rvfStatus = "Completed";
			}
		}
		final BuildReport report = build.getBuildReport();
		LOGGER.info("RVF Result: {}", rvfResultMsg);
		report.add("post_validation_status", rvfStatus);
		report.add("rvf_response", rvfResultMsg);
		LOGGER.info("End of running build {}", build.getUniqueId());
		dao.persistReport(build);
	}

	private void generateReleasePackageFile(Build build, String releaseFilename) throws BusinessServiceException {
		try {
			LOGGER.info("Generating release package information file for build {}", build.getUniqueId());
			Map<String, Object> releasePackageInformationMap = getReleasePackageInformationMap(build);
			FileWriter fileWriter = null;
			File releasePackageInfoFile;
			try {
				releasePackageInfoFile = new File(releaseFilename);
				fileWriter = new FileWriter(releasePackageInfoFile, StandardCharsets.UTF_8);

				Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
				JsonElement je = JsonParser.parseString(mapToString(releasePackageInformationMap));
				fileWriter.write(gson.toJson(je));
			} finally {
				fileWriter.flush();
				fileWriter.close();
			}
			try {
				if (releasePackageInfoFile != null) {
					dao.putOutputFile(build, releasePackageInfoFile);
				}
			} finally {
				if (releasePackageInfoFile != null) {
					releasePackageInfoFile.delete();
				}
			}
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to generate release package information file.", e);
		} catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

	private <K, V> String mapToString(Map<K, V> map) {
		return map.entrySet()
				.stream()
				.map(entry -> entry.getKey() + ":" + (entry.getValue() instanceof String ? "\"" + StringEscapeUtils.escapeJson(entry.getValue().toString()) + "\"" : entry.getValue()))
				.collect(Collectors.joining(", ", "{", "}"));
	}

	private String getReleaseFilename(Build build) {
		String releaseFilename = null;
		try {
			final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
			final InputStream manifestStream = dao.getManifestStream(build);
			final ListingType manifestListing = unmarshaller.unmarshal(new StreamSource(manifestStream), ListingType.class).getValue();

			if (manifestListing != null) {
				final FolderType rootFolder = manifestListing.getFolder();
				if (rootFolder != null) {
					final List<FileType> files = rootFolder.getFile();
					for (final FileType file : files) {
						final String filename = file.getName();
						if (filename != null && filename.toLowerCase().startsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_PREFIX) && filename.endsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_EXTENSION)) {
							releaseFilename = filename;
							break;
						}
					}
				}
			} else {
				LOGGER.warn("Can not generate release package information file, manifest listing is null.");
			}
		} catch (JAXBException e) {
			throw new BusinessServiceRuntimeException("Failed to get filenames from the manifest.xml.", e);
		}

		return releaseFilename;
	}

	private Map<String, String> getPreferredTermMap(Build build) {
		Map<String, String> result = new HashMap<>();
		if (StringUtils.hasLength(build.getConfiguration().getConceptPreferredTerms())) {
			String[] conceptIdAndTerms = build.getConfiguration().getConceptPreferredTerms().split(",");
			for (String conceptIdAndTerm : conceptIdAndTerms) {
				String[] arr = conceptIdAndTerm.split(Pattern.quote("|"));
				if (arr.length != 0) {
					result.put(arr[0].trim(), arr[1].trim());
				}
			}
		}
		return result;
	}

	private Map<String, Object> getReleasePackageInformationMap(Build build) throws JSONException {
		Map<String, Object> result = new LinkedHashMap<>();

		BuildConfiguration buildConfig = build.getConfiguration();
		List<RefsetType> languagesRefsets = getLanguageRefsets(build);
		Map<String, Integer> deltaFromAndToDateMap = getDeltaFromAndToDate(build);
		Map<String, String> preferredTermMap = getPreferredTermMap(build);

		if (StringUtils.hasLength(buildConfig.getAdditionalReleaseInformationFields())) {
			JSONObject jsonObject = parseAdditionalReleaseInformationJSON(buildConfig.getAdditionalReleaseInformationFields());
			Iterator<String> iterator = jsonObject.keys();
			while(iterator.hasNext()) {
				String key = iterator.next();
				result.put(key, jsonObject.get(key));
			}
			for (String key : result.keySet()) {
				if (JSONObject.NULL.equals(result.get(key))) {
                    switch (key.trim()) {
                        case "effectiveTime" ->
                                result.put("effectiveTime", buildConfig.getEffectiveTime() != null ? buildConfig.getEffectiveTimeSnomedFormat() : null);
                        case "deltaFromDate" -> {
                            Integer deltaFromDateInt = deltaFromAndToDateMap.get("deltaFromDate");
                            result.put("deltaFromDate", deltaFromDateInt != null ? deltaFromDateInt.toString() : null);
                        }
                        case "deltaToDate" -> {
                            Integer deltaToDateInt = deltaFromAndToDateMap.get("deltaToDate");
                            result.put("deltaToDate", deltaToDateInt != null ? deltaToDateInt.toString() : null);
                        }
                        case "previousPublishedPackage" ->
                                result.put("previousPublishedPackage", buildConfig.getPreviousPublishedPackage());
                        case "includedModules" -> {
                            Set<String> extensionModules = buildConfig.getExtensionConfig() != null ? buildConfig.getExtensionConfig().getModuleIdsSet() : null;
                            if (extensionModules != null) {
                                List<ConceptMini> list = new ArrayList<>();
                                for (String moduleId : extensionModules) {
                                    ConceptMini conceptMini = new ConceptMini();
                                    moduleId = moduleId.trim();
                                    conceptMini.setId(moduleId);
                                    conceptMini.setTerm(preferredTermMap.getOrDefault(moduleId, ""));
                                    list.add(conceptMini);
                                }
                                result.put("includedModules", list);
                            }
                        }
                        case "languageRefsets" -> {
                            List<ConceptMini> list = new ArrayList<>();
                            for (RefsetType refsetType : languagesRefsets) {
                                ConceptMini conceptMini = new ConceptMini();
                                String languageRefsetId = String.valueOf(refsetType.getId()).trim();
                                conceptMini.setId(languageRefsetId);
                                conceptMini.setTerm(preferredTermMap.containsKey(languageRefsetId) ? preferredTermMap.get(languageRefsetId) : refsetType.getLabel());
                                list.add(conceptMini);
                            }
                            result.put("languageRefsets", list);
                        }
                        case "licenceStatement" ->
                                result.put("licenceStatement", buildConfig.getLicenceStatement() != null ? buildConfig.getLicenceStatement() : "");
                        default -> {
                        }
                    }
				}
			}
		}

		return result;
	}

	private JSONObject parseAdditionalReleaseInformationJSON(String additionalFields) {
		try {
			return new JSONObject(additionalFields) {
				/**
				 * changes the value of JSONObject.map to a LinkedHashMap in order to maintain
				 * order of keys.
				 */
				@Override
				public JSONObject put(String key, Object value) throws JSONException {
					try {
						Field map = JSONObject.class.getDeclaredField("map");
						map.setAccessible(true);
						Object mapValue = map.get(this);
						if (!(mapValue instanceof LinkedHashMap)) {
							map.set(this, new LinkedHashMap<>());
						}
					} catch (NoSuchFieldException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
					return super.put(key, value);
				}
			};
		} catch (JSONException ex) {
			throw new BusinessServiceRuntimeException("Failed to parse the additional fields to JSON object.", ex);
		}
	}

	private List<RefsetType> getLanguageRefsets(Build build) {
		List<RefsetType> languagesRefsets = new ArrayList<>();
		try (InputStream manifestInputSteam = dao.getManifestStream(build)) {
			final ManifestXmlFileParser parser = new ManifestXmlFileParser();
			final ListingType listingType = parser.parse(manifestInputSteam);
			FolderType folderType = listingType.getFolder();
			List<FolderType> folderTypes = folderType.getFolder();
			boolean isDeltaFolderExistInManifest = false;
			for (FolderType subFolderType1 : folderTypes) {
				if (subFolderType1.getName().equalsIgnoreCase(RF2Constants.DELTA)) {
					isDeltaFolderExistInManifest = true;
					break;
				}
			}
			for (FolderType subFolderType1 : folderTypes) {
				if ((isDeltaFolderExistInManifest && subFolderType1.getName().equalsIgnoreCase(RF2Constants.DELTA))
					|| (!isDeltaFolderExistInManifest && subFolderType1.getName().equalsIgnoreCase(RF2Constants.SNAPSHOT)) ) {
					for (FolderType subFolderType2 : subFolderType1.getFolder()) {
						if (subFolderType2.getName().equalsIgnoreCase(RF2Constants.REFSET)) {
							for (FolderType subFolderType3 : subFolderType2.getFolder()) {
								if (subFolderType3.getName().equalsIgnoreCase(RF2Constants.LANGUAGE)) {
									List<FileType> fileTypes = subFolderType3.getFile();
									for (FileType fileType : fileTypes) {
										ContainsReferenceSetsType refset = fileType.getContainsReferenceSets();
										if (refset != null) {
											languagesRefsets.addAll(refset.getRefset());
										}
									}
									break;
								}
							}
							break;
						}
					}
					break;
				}
			}
		} catch (ResourceNotFoundException | JAXBException | IOException e) {
			LOGGER.error("Failed to parse manifest xml file." + e.getMessage());
		}

		return languagesRefsets;
	}

	private Map<String, Integer> getDeltaFromAndToDate(Build build) {
		Map<String, Integer> result = new HashMap<>();
		BuildConfiguration configuration = build.getConfiguration();
		String previousReleaseDateStr = null;
		if (configuration.getPreviousPublishedPackage() != null && !configuration.getPreviousPublishedPackage().isEmpty()) {
			String[] tokens = build.getConfiguration().getPreviousPublishedPackage().split(RF2Constants.FILE_NAME_SEPARATOR);
			if (tokens.length > 0) {
				previousReleaseDateStr = tokens[tokens.length - 1].replace(RF2Constants.ZIP_FILE_EXTENSION, "");
				try {
					Date preReleasedDate = RF2Constants.DATE_FORMAT.parse(previousReleaseDateStr);
					previousReleaseDateStr = RF2Constants.DATE_FORMAT.format(preReleasedDate); // make sure the date in format yyyyMMdd
				} catch (ParseException e) {
					LOGGER.error("Expecting release date format in package file name to be yyyyMMdd");
					previousReleaseDateStr = null;
				}
			}
		}

		result.put("deltaFromDate", previousReleaseDateStr != null ? Integer.valueOf(previousReleaseDateStr) : null);
		result.put("deltaToDate", Integer.valueOf(RF2Constants.DATE_FORMAT.format(configuration.getEffectiveTime())));

		return result;
	}

	/**
	 * Manifest.xml can have delta, snapshot or Full only and all three combined.
	 *
	 * @param build
	 * @return
	 */
	private List<String> rf2DeltaFilesSpecifiedByManifest(Build build) {
		List<String> result = new ArrayList<>();
		try (InputStream manifestInputSteam = dao.getManifestStream(build)) {
			final ManifestXmlFileParser parser = new ManifestXmlFileParser();
			final ListingType listingType = parser.parse(manifestInputSteam);
			Set<String> filesRequested = new HashSet<>();
			for (String fileName : ManifestFileListingHelper.listAllFiles(listingType)) {
				if (fileName != null && fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
					if (fileName.contains(RF2Constants.DELTA + RF2Constants.FILE_NAME_SEPARATOR) || fileName.contains(RF2Constants.DELTA + HYPHEN)) {
						filesRequested.add(fileName);
					} else if (fileName.contains(RF2Constants.SNAPSHOT + RF2Constants.FILE_NAME_SEPARATOR) || fileName.contains(RF2Constants.SNAPSHOT + HYPHEN)) {
						filesRequested.add(fileName.replace(RF2Constants.SNAPSHOT, RF2Constants.DELTA));
					} else if (fileName.contains(RF2Constants.FULL + RF2Constants.FILE_NAME_SEPARATOR) || fileName.contains(RF2Constants.FULL + HYPHEN)) {
						filesRequested.add(fileName.replace(RF2Constants.FULL, RF2Constants.DELTA));
					}
				}
			}
			//changed to rel2 input files format
			for (String delta : filesRequested) {
				String[] splits = delta.split(RF2Constants.FILE_NAME_SEPARATOR);
				splits[0] = RF2Constants.INPUT_FILE_PREFIX;
				StringBuilder relFileBuilder = new StringBuilder();
				for (int i = 0; i < splits.length; i++) {
					if (i > 0) {
						relFileBuilder.append(RF2Constants.FILE_NAME_SEPARATOR);
					}
					relFileBuilder.append(splits[i]);
				}
				String relFileName = relFileBuilder.toString();
				if (!Normalizer.isNormalized(relFileName, Form.NFC)) {
					relFileName = Normalizer.normalize(relFileBuilder.toString(), Form.NFC);
				}
				result.add(relFileName);
			}
		} catch (ResourceNotFoundException | JAXBException | IOException e) {
			LOGGER.error("Failed to parse manifest xml file.", e);
		}
		return result;
	}

	private void retrieveAdditionalRelationshipsInputDelta(final Build build, String inferredDelta) throws BusinessServiceException {
		LOGGER.debug("Retrieving inactive additional relationship from transformed delta {}", inferredDelta);
		String originalDelta = inferredDelta + "_original";
		String additionalRelsDelta = inferredDelta.replace(RF2Constants.TXT_FILE_EXTENSION, RF2Constants.ADDITIONAL_TXT);
		dao.renameTransformedFile(build, inferredDelta, originalDelta, false);
		try (final OutputStream outputStream = dao.getTransformedFileOutputStream(build, additionalRelsDelta).getOutputStream();
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, RF2Constants.UTF_8))) {
			final InputStream inputStream = dao.getTransformedFileAsInputStream(build, originalDelta);
			if (inputStream != null) {
				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
					String line;
					boolean isFirstLine = true;
					while ((line = reader.readLine()) != null) {
						if (isFirstLine) {
							writer.write(line);
							writer.write(RF2Constants.LINE_ENDING);
							isFirstLine = false;
						}
						String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR);
						if (ADDITIONAL_RELATIONSHIP.equals(columnValues[8]) && RF2Constants.BOOLEAN_FALSE.equals(columnValues[2])) {
							writer.write(line);
							writer.write(RF2Constants.LINE_ENDING);
						}
					}
				}
			}
		} catch (final IOException e) {
			throw new BusinessServiceException("Error occurred when reading original relationship delta transformed file:" + originalDelta, e);
		}
	}

	private String getInferredDeltaFromInput(final Map<String, TableSchema> inputFileSchemaMap) {
		for (final String inputFilename : inputFileSchemaMap.keySet()) {
			final TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);

			if (inputFileSchema == null) {
				continue;
			}

			if (inputFileSchema.getComponentType() == ComponentType.RELATIONSHIP) {
				return inputFilename;
			}
		}
		return null;
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

	private String runRVFPostConditionCheck(final Build build, final String s3ZipFilePath, String manifestFileS3Path, Integer failureExportMax) throws IOException {
		LOGGER.info("Initiating RVF post-condition check for zip file {} with failureExportMax param value {}", s3ZipFilePath, failureExportMax);
		String rvfResponse = null;
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl)) {
			final QATestConfig qaTestConfig = build.getQaTestConfig();
			// Has the client told us where to tell the RVF to store the results? Set if not
			if (qaTestConfig.getStorageLocation() == null || qaTestConfig.getStorageLocation().isEmpty()) {
				final String storageLocation = build.getReleaseCenterKey()
						+ "/" + build.getProductKey()
						+ "/" + build.getId();
				qaTestConfig.setStorageLocation(storageLocation);
			}
			BuildConfiguration buildConfiguration = build.getConfiguration();
			validateQaTestConfig(qaTestConfig, buildConfiguration);
			String effectiveTime = buildConfiguration.getEffectiveTimeFormatted();
			boolean releaseAsAnEdition = false;
			String includedModuleIdsStr = null;
			ExtensionConfig extensionConfig = buildConfiguration.getExtensionConfig();
			if (extensionConfig != null) {
				releaseAsAnEdition = extensionConfig.isReleaseAsAnEdition();
				includedModuleIdsStr = extensionConfig.getModuleIds();
			}

			String defaultModuleId = null;
			if (extensionConfig != null) {
				if (StringUtils.hasLength(extensionConfig.getDefaultModuleId())) {
					defaultModuleId = extensionConfig.getDefaultModuleId();
				} else {
					if (!CollectionUtils.isEmpty(extensionConfig.getModuleIdsSet()) && extensionConfig.getModuleIdsSet().size() == 1) {
						defaultModuleId = extensionConfig.getModuleIdsSet().iterator().next();
					}
				}
			}

			String runId = Long.toString(System.currentTimeMillis());
			ValidationRequest request = new ValidationRequest(runId);
			request.setBuildBucketName(buildBucketName);
			request.setReleaseZipFileS3Path(s3ZipFilePath);
			request.setEffectiveTime(effectiveTime);
			request.setPreviousPublishedPackage(buildConfiguration.getPreviousPublishedPackage());
			request.setExtensionDependencyRelease(buildConfiguration.getExtensionConfig() != null ? buildConfiguration.getExtensionConfig().getDependencyRelease() : null);
			request.setPreviousExtensionDependencyEffectiveTime(buildConfiguration.getExtensionConfig() != null ? buildConfiguration.getExtensionConfig().getPreviousEditionDependencyEffectiveDateFormatted() : null);
			request.setFailureExportMax(failureExportMax);
			request.setManifestFileS3Path(manifestFileS3Path);
			request.setReleaseAsAnEdition(releaseAsAnEdition);
			request.setStandAloneProduct(buildConfiguration.isStandAloneProduct());
			request.setDailyBuild(buildConfiguration.isDailyBuild());
			request.setDefaultModuleId(defaultModuleId);
			request.setIncludedModuleIds(includedModuleIdsStr);
			request.setResponseQueue(queue);
			request.setBranchPath(buildConfiguration.getBranchPath());
			request.setExcludedRefsetDescriptorMembers(buildConfiguration.getExcludeRefsetDescriptorMembers());
			sendMiniRvfValidationRequestToBuildStatusMessage(build, runId);
			rvfResponse = rvfClient.validateOutputPackageFromS3(qaTestConfig, request);
			if (buildConfiguration.isDailyBuild()) {
				sendDailyBuildRvfResponseUpdateMessage(buildConfiguration.getBranchPath(), rvfResponse);
			}
		} catch (IOException | BusinessServiceException | ConfigurationException e) {
			LOGGER.error("Failed to run RVF validations.", e);
			dao.updateStatus(build, Status.RVF_FAILED);
		}
		return rvfResponse;
	}

	private void sendMiniRvfValidationRequestToBuildStatusMessage(final Build build, final String runId) {
		messagingHelper.sendResponse(buildStatusTextMessage,
				ImmutableMap.of("runId", Long.valueOf(runId),
						"buildId", build.getId(),
						"releaseCenterKey", build.getReleaseCenterKey(),
						"productKey", build.getProductKey()));
	}

	private void sendDailyBuildRvfResponseUpdateMessage(String branchPath, String rvfURL) {
		try {
			messagingHelper.send(new ActiveMQQueue(dailyBuildRvfResponseQueue), ImmutableMap.of("rvfURL",rvfURL,
					"branchPath", branchPath));
		} catch (JsonProcessingException | JMSException e) {
			LOGGER.error("Failed to send daily build RVF response notification for {}.", branchPath, e);
		}
	}

	private void validateQaTestConfig(final QATestConfig qaTestConfig, final BuildConfiguration buildConfig) throws ConfigurationException {
		if (qaTestConfig == null || qaTestConfig.getAssertionGroupNames() == null) {
			throw new ConfigurationException("No QA test configured. Please check the assertion group name is specified.");
		}
		if (!buildConfig.isJustPackage() && !buildConfig.isFirstTimeRelease()) {
			if (buildConfig.getExtensionConfig() == null && buildConfig.getPreviousPublishedPackage() == null) {
				throw new ConfigurationException("No previous international release is configured for non-first time release.");
			}
			if (buildConfig.getExtensionConfig() != null && buildConfig.getExtensionConfig().getDependencyRelease() != null && buildConfig.getPreviousPublishedPackage() == null) {
				throw new ConfigurationException("Extension dependency release is specified but no previous extension release is configured for non-first time release testing.");
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
		List<String> rf2DeltaFilesFromManifest = rf2DeltaFilesSpecifiedByManifest(build);
		for (String fileInManifest : rf2DeltaFilesFromManifest) {
			LOGGER.debug(fileInManifest);
		}
		final Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		for (final String buildInputFilePath : buildInputFilePaths) {
			final TableSchema schemaBean;
			try {
				String filename = FileUtils.getFilenameFromPath(buildInputFilePath);
				if (!Normalizer.isNormalized(filename, Form.NFC)) {
					filename = Normalizer.normalize(filename, Form.NFC);
				}
				//Filtered out any files not required by Manifest.xml
				if (rf2DeltaFilesFromManifest.contains(filename)) {
					schemaBean = schemaFactory.createSchemaBean(filename);
					inputFileSchemaMap.put(buildInputFilePath, schemaBean);
					LOGGER.debug("getInputFileSchemaMap {} - {}", filename, schemaBean != null ? schemaBean.getTableName() : null);
				} else {
					LOGGER.info("RF2 filename {} has not been specified in the manifest.xml", filename);
				}
			} catch (final FileRecognitionException e) {
				throw new BusinessServiceException("Did not recognise input file '" + buildInputFilePath + "'", e);
			}
		}
		return inputFileSchemaMap;
	}

	private Build getBuildOrThrow(final String releaseCenterKey, final String productKey, final String buildId) throws ResourceNotFoundException {
		final Build build = dao.find(releaseCenterKey, productKey, buildId, null, null, null, null);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildId + " for product: " + productKey);
		}
		return build;
	}

	private Build getBuild(final String releaseCenterKey, final String productKey, final Date creationTime) {
		return dao.find(releaseCenterKey, productKey, EntityHelper.formatAsIsoDateTime(creationTime), null, null, null, null);
	}

	private Product getProduct(final String releaseCenterKey, final String productKey) throws ResourceNotFoundException {
		return productDAO.find(releaseCenterKey, productKey);
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
						if (file.getName().startsWith(RF2Constants.README_FILENAME_PREFIX) && filename.endsWith(RF2Constants.README_FILENAME_EXTENSION)) {
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
						RF2Constants.README_FILENAME_PREFIX, RF2Constants.README_FILENAME_EXTENSION);
			}
		} catch (IOException | ExecutionException | JAXBException | InterruptedException e) {
			throw new BusinessServiceException("Failed to generate readme file.", e);
		}
	}

	@Override
	public QATestConfig loadQATestConfig(final String releaseCenterKey, final String productKey, final String buildId) throws BusinessServiceException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		try {
			dao.loadQaTestConfig(build);
			return build.getQaTestConfig();
		} catch (final IOException e) {
			throw new BusinessServiceException("Failed to load QA test configuration.", e);
		}
	}

	@Override
	public InputStream getBuildReportFile(Build build) throws ResourceNotFoundException {
		return dao.getBuildReportFileStream(build);
	}

	@Override
	public InputStream getBuildReportFile(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		LOGGER.info("Build Used To Find Build Report File: {}", build);
		return dao.getBuildReportFileStream(build);
	}

	@Override
	public InputStream getBuildInputFilesPrepareReport(String releaseCenterKey, String productKey, String buildId) {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getBuildInputFilesPrepareReportStream(build);
	}

	@Override
	public void requestCancelBuild(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException, BadConfigurationException, IOException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		//Only cancel build if the status is "QUEUED" or "BEFORE_TRIGGER" or "BUILDING"
		if (Status.BUILDING != build.getStatus() && Status.QUEUED != build.getStatus() && Status.BEFORE_TRIGGER != build.getStatus() ) {
			throw new BadConfigurationException("Build " + build.getCreationTime() + " is at status: " + build.getStatus().name()
					+ " and is expected to be at status:" + Status.QUEUED.name() + " or " +  Status.BEFORE_TRIGGER.name() + " or " + Status.BUILDING.name());
		}
		dao.updateStatus(build, Status.CANCEL_REQUESTED);
		LOGGER.warn("Status of build {} has been updated to {}", build, Status.CANCEL_REQUESTED.name());
	}

	@Override
	public InputStream getBuildInputGatherReport(String releaseCenterKey, String productKey, String buildId) {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getBuildInputGatherReportStream(build);
	}

	@Override
	public InputStream getPreConditionChecksReport(String releaseCenterKey, String productKey, String buildId) {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getPreConditionCheckReportStream(build);
	}

	@Override
	public InputStream getPostConditionChecksReport(String releaseCenterKey, String productKey, String buildId) {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getPostConditionCheckReportStream(build);
	}

	@Override
	public List<String> getClassificationResultOutputFilePaths(String releaseCenterKey, String productKey, String buildId) {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.listClassificationResultOutputFileNames(build);
	}

	@Override
	public InputStream getClassificationResultOutputFile(String releaseCenterKey, String productKey, String buildId, String inputFilePath) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		return dao.getClassificationResultOutputFileStream(build, inputFilePath);
	}

	@Override
	public void updateVisibility(String releaseCenterKey, String productKey, String buildId, boolean visibility) throws IOException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		updateVisibility(build, visibility);
	}

	@Override
	public void updateVisibility(Build build, boolean visibility) throws IOException {
		dao.updateVisibility(build, visibility);
	}

	@Override
	public void saveTags(Build build, List<Build.Tag> tags) throws IOException {
		dao.saveTags(build, tags);
	}

	@Override
	public Build cloneBuild(final String releaseCenterKey, final String productKey, final String buildId, final String username) throws BusinessServiceException {
		Build build;
		String sourceBuildPath;
		String sourceBucketName;
		try {
			build = this.find(releaseCenterKey, productKey, buildId, true, true, null , null);
			sourceBuildPath = pathHelper.getBuildPath(build).toString();
			sourceBucketName = this.buildBucketName;
		} catch (ResourceNotFoundException e) {
			List<Build> publishedBuilds = publishService.findPublishedBuilds(releaseCenterKey, productKey, true);
			build = publishedBuilds.stream().filter(b -> b.getId().equals(buildId)).findAny().orElse(null);
			if (build != null) {
				Map<String, String> buildPathMap = publishService.getPublishedBuildPathMap(releaseCenterKey, productKey);
				String absoluteBuildPath = buildPathMap.get(buildId);
				sourceBuildPath = absoluteBuildPath.substring(absoluteBuildPath.indexOf(S3PathHelper.SEPARATOR) + 1);
				sourceBucketName = absoluteBuildPath.substring(0, absoluteBuildPath.indexOf(S3PathHelper.SEPARATOR));
			} else {
				throw e;
			}
		}

		final Date creationDate = new Date();
		// Do we already have an build for that date?
		final Build existingBuild = getBuild(build.getReleaseCenterKey(), build.getProductKey(), creationDate);
		if (existingBuild != null) {
			throw new EntityAlreadyExistsException("A build for product " + build.getProductKey() + " already exists with build id " + existingBuild.getId());
		}

		Build newBuild;
		final BuildConfiguration buildConfiguration =  build.getConfiguration();
		final QATestConfig qaTestConfig =  build.getQaTestConfig();

		try {
			newBuild = new Build(creationDate, build.getReleaseCenterKey(), build.getProductKey(), buildConfiguration, qaTestConfig);
			newBuild.setBuildUser(username);

			// Copy build and qa configurations
			buildConfiguration.setBuildName(build.getId() + " - clone");
			buildConfiguration.setExportType(null);
			buildConfiguration.setLoadExternalRefsetData(false);
			buildConfiguration.setLoadTermServerData(false);

			newBuild.setConfiguration(buildConfiguration);
			newBuild.setQaTestConfig(qaTestConfig);

			// create build status tracker
			BuildStatusTracker tracker = new BuildStatusTracker();
			tracker.setProductKey(newBuild.getProductKey());
			tracker.setReleaseCenterKey(newBuild.getReleaseCenterKey());
			tracker.setBuildId(newBuild.getId());
			statusTrackerDao.save(tracker);

			dao.save(newBuild);

			// Copy input-files and manifest from the old build
			String destBuildPath = pathHelper.getBuildPath(newBuild).toString();
			dao.copyBuildToAnother(sourceBucketName, sourceBuildPath, this.buildBucketName, destBuildPath, "input-files");
			dao.copyBuildToAnother(sourceBucketName, sourceBuildPath, this.buildBucketName, destBuildPath, "manifest");
		} catch (Exception e) {
			throw new BusinessServiceException("Failed to create build for product " + build.getProductKey(), e);
		}

		return newBuild;
	}

	@Override
	public String getManifestFileName(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		LOGGER.info("Build used to find manifest file: {}", build);
		return dao.getManifestFilePath(build);
	}

	@Override
	public InputStream getManifestStream(String releaseCenterKey, String productKey, String buildId) throws ResourceNotFoundException {
		final Build build = getBuildOrThrow(releaseCenterKey, productKey, buildId);
		LOGGER.info("Build used to find manifest file: {}", build);
		return dao.getManifestStream(build);
	}
}
