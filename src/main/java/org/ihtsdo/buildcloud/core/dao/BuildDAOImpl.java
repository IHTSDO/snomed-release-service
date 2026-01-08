package org.ihtsdo.buildcloud.core.dao;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import io.awspring.cloud.s3.ObjectMetadata;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.helper.ListHelper;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.compare.BuildComparisonReport;
import org.ihtsdo.buildcloud.core.service.build.compare.FileDiffReport;
import org.ihtsdo.buildcloud.core.service.helper.Rf2FileNameTransformation;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.buildcloud.telemetry.core.TelemetryStreamPathBuilder;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.ihtsdo.buildcloud.core.entity.Build.Tag;

@Service
public class BuildDAOImpl implements BuildDAO {

	private static final String BLANK = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildDAOImpl.class);

	private static final String INTERNATIONAL = "international";

	private final ExecutorService executorService;

	private final FileHelper srsFileHelper;

	private final ObjectMapper objectMapper;

	private final File tempDir;

	private final Rf2FileNameTransformation rf2FileNameTransformation;

	private final S3Client s3Client;

	private final MessagingHelper messagingHelper;

	private final ActiveMQTextMessage buildStatusTextMessage;

	private final S3PathHelper pathHelper;

	private final String buildBucketName;

	private final InputFileDAO inputFileDAO;

	@Value("${srs.published.releases.storage.path}")
	private String publishedReleasesStoragePath;

	@Value("${srs.publish.job.storage.path}")
	private String publishJobStoragePath;

	private final Map<String, Set<String>> buildIdsToProductMap = new ConcurrentHashMap<>();

	private static final List<String> requiredFileExtensions = Arrays.asList("status:", "tag:", "user:", "user-roles:", "visibility:", S3PathHelper.MARK_AS_DELETED);

	private static final String RVF_RESPONSE_KEY = "rvf_response";

	private static final String SORT_PROPERTY_BUILD_NAME = "buildName";

	private static final String SORT_PROPERTY_CREATION_TIME = "creationTime";

	private static final String SORT_PROPERTY_STATUS = "status";

	private static final String SORT_PROPERTY_BUILD_USER = "buildUser";

    private static final String MANIFEST_DIR_PREFIX = "manifest" + S3PathHelper.SEPARATOR;

	private static final Set<Build.Status> TERMINAL_STATUSES = EnumSet.of(
			Build.Status.FAILED_INPUT_GATHER_REPORT_VALIDATION,
			Build.Status.FAILED_INPUT_PREPARE_REPORT_VALIDATION,
			Build.Status.FAILED_PRE_CONDITIONS,
			Build.Status.FAILED_POST_CONDITIONS,
			Build.Status.CANCELLED,
			Build.Status.FAILED
	);

	private record BuildRequestParameter (
			String releaseCenterKey,
			String productKey,
			Boolean includeBuildConfiguration,
			Boolean includeQAConfiguration,
			Boolean includeRvfURL,
			Boolean visibility,
			BuildService.View viewMode,
			List<Integer> forYears,
			PageRequest pageRequest) {}
	private record BuildResponse (
			List<Build> builds,
			List<String> userPaths,
			List<String> userRolesPaths,
			List<String> tagPaths,
			List<String> visibilityPaths,
			List<String> buildsMarkAsDeleted
	) {}

	private record GetS3ObjectResponse (
			List<S3Object> s3Objects,
			Boolean isGetAllBuild
	) {}

	@Autowired
	public BuildDAOImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
						final S3Client s3Client,
						final MessagingHelper messagingHelper,
						final ObjectMapper objectMapper,
						final S3PathHelper pathHelper,
						final InputFileDAO inputFileDAO,
						final ActiveMQTextMessage buildStatusTextMessage) throws IOException {
		executorService = Executors.newCachedThreadPool();
		buildBucketName = storageBucketName;
		srsFileHelper = new FileHelper(storageBucketName, s3Client);
		this.tempDir = Files.createTempDirectory("srs-temp").toFile();
		rf2FileNameTransformation = new Rf2FileNameTransformation();
		this.s3Client = s3Client;
        this.messagingHelper = messagingHelper;
        this.objectMapper = objectMapper;
        this.pathHelper = pathHelper;
        this.inputFileDAO = inputFileDAO;
        this.buildStatusTextMessage = buildStatusTextMessage;
	}

	@Override
	public void copyManifestFileFromProduct(Build build) {
		// Copy manifest file
		final String manifestPath = inputFileDAO.getManifestPath(build.getReleaseCenterKey(), build.getProductKey());
		if (manifestPath != null) {
			final String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
			final String manifestFileName = Paths.get(manifestPath).getFileName().toString();
			srsFileHelper.copyFile(manifestPath, buildManifestDirectoryPath + manifestFileName);
		}
	}

	@Override
	public void save(final Build build) throws IOException {
		// Save config file
		LOGGER.debug("Saving build {} for product {}", build.getId(), build.getProductKey());
		File configJson = toJson(build.getConfiguration());
		File qaConfigJson = toJson(build.getQaTestConfig());
		try (FileInputStream buildConfigInputStream = new FileInputStream(configJson);
			 FileInputStream qaConfigInputStream = new FileInputStream(qaConfigJson)) {
			s3Client.putObject(buildBucketName, pathHelper.getBuildConfigFilePath(build), buildConfigInputStream, ObjectMetadata.builder().build(), configJson.length());
			s3Client.putObject(buildBucketName, pathHelper.getQATestConfigFilePath(build), qaConfigInputStream, ObjectMetadata.builder().build(), qaConfigJson.length());
		} finally {
			if (configJson != null) {
                Files.deleteIfExists(configJson.toPath());
			}
			if (qaConfigJson != null) {
                Files.deleteIfExists(qaConfigJson.toPath());
			}
		}

		// Save trigger user
		if (StringUtils.isNotEmpty(build.getBuildUser())) {
			final String userFilePath = pathHelper.getBuildUserFilePath(build, build.getBuildUser());
			putFile(userFilePath, BLANK);
		}

		// Save trigger user roles
		if (!CollectionUtils.isEmpty(build.getUserRoles())) {
			final String userRolesFilePath = pathHelper.getBuildUserRolesFilePath(build, build.getUserRoles());
			putFile(userRolesFilePath, BLANK);
		}

		// save build status
		final String newStatusFilePath = pathHelper.getStatusFilePath(build, build.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);

		this.buildIdsToProductMap.computeIfAbsent(build.getReleaseCenterKey() + org.ihtsdo.otf.RF2Constants.DASH + build.getProductKey(), k -> new HashSet<>()).add(build.getId());
		LOGGER.debug("Saved build {}", build.getId());
	}

	@Override
	public void updateQATestConfig(final Build build) throws IOException {
		File qaConfigJson = toJson(build.getQaTestConfig());
		try (FileInputStream qaConfigInputStream = new FileInputStream(qaConfigJson)) {
			s3Client.deleteObject(buildBucketName, pathHelper.getQATestConfigFilePath(build));
			s3Client.putObject(buildBucketName, pathHelper.getQATestConfigFilePath(build), qaConfigInputStream, ObjectMetadata.builder().build(), qaConfigJson.length());
		} finally {
			if (qaConfigJson != null) {
                Files.deleteIfExists(qaConfigJson.toPath());
			}
		}
	}

	@Override
	public void updateBuildConfiguration(final Build build) throws IOException {
		File configJson = toJson(build.getConfiguration());
		try (FileInputStream buildConfigInputStream = new FileInputStream(configJson)) {
			s3Client.deleteObject(buildBucketName, pathHelper.getBuildConfigFilePath(build));
			s3Client.putObject(buildBucketName, pathHelper.getBuildConfigFilePath(build), buildConfigInputStream, ObjectMetadata.builder().build(), configJson.length());
		} finally {
			if (configJson != null) {
                Files.deleteIfExists(configJson.toPath());
			}
		}
	}


	protected File toJson(final Object obj) throws IOException {
		final File temp = File.createTempFile("tempJson", ".tmp");
		objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
		final JsonFactory jsonFactory = objectMapper.getFactory();
		try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(temp, JsonEncoding.UTF8)) {
			jsonGenerator.writeObject(obj);
		}
		return temp;
	}

	@Override
	public List<Build> findAllDesc(final String releaseCenterKey, final String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) {
		final String productDirectoryPath = pathHelper.getProductPath(releaseCenterKey, productKey).toString();
		final BuildRequestParameter requestParameter = new BuildRequestParameter(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility, null, null, null);
		return findBuildsDesc(productDirectoryPath, requestParameter);
	}

	@Override
	public BuildPage<Build> findAll(final String releaseCenterKey, final String productKey, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility, BuildService.View viewMode, List<Integer> forYears, PageRequest pageRequest) {
		final String productDirectoryPath = pathHelper.getProductPath(releaseCenterKey, productKey).toString();
		final BuildRequestParameter requestParameter = new BuildRequestParameter(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility, viewMode, forYears, pageRequest);
		return findBuilds(productDirectoryPath, requestParameter);
	}

	@Override
	public Build find(final String releaseCenterKey, final String productKey, final String buildId, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) {
		final String buildDirectoryPath = pathHelper.getBuildPath(releaseCenterKey, productKey, buildId).toString();
		final BuildRequestParameter requestParameter = new BuildRequestParameter(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility, null, null, null);
		final List<Build> builds = findBuildsDesc(buildDirectoryPath, requestParameter);
		if (!builds.isEmpty()) {
			return builds.get(0);
		} else {
			return null;
		}
	}

	@Override
	public void delete(String releaseCenterKey, String productKey, String buildId) {
		String buildDirectoryPath = pathHelper.getBuildPath(releaseCenterKey, productKey, buildId).toString();
		List<String> filenames = srsFileHelper.listFiles(buildDirectoryPath);
		for (String filename : filenames) {
			s3Client.deleteObject(buildBucketName, buildDirectoryPath + filename);
		}
	}

	@Override
	public void markBuildAsDeleted(Build build) {
		final String newTagFilePath = pathHelper.getBuildPath(build).append(S3PathHelper.MARK_AS_DELETED).toString();
		putFile(newTagFilePath, BLANK);
	}

	@Override
	public void clearBuildIdsCache() {
		this.buildIdsToProductMap.clear();
	}

	@Override
	public void loadBuildConfiguration(final Build build) throws IOException {
		final String configFilePath = pathHelper.getBuildConfigFilePath(build);
		try (final InputStream inputStream = s3Client.getObject(buildBucketName, configFilePath)){
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(inputStream, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final BuildConfiguration buildConfiguration = jsonParser.readValueAs(BuildConfiguration.class);
				build.setConfiguration(buildConfiguration);
			}
		} catch (S3Exception e) {
			if (404 == e.statusCode()) {
				throw new ResourceNotFoundException("Build configuration file is missing from the build '" + build.getId()+ "'.");
			} else {
				throw e;
			}
		}
	}


	@Override
	public void loadQaTestConfig(final Build build) throws IOException {
		final String configFilePath = pathHelper.getQATestConfigFilePath(build);
		try (final InputStream inputStream = s3Client.getObject(buildBucketName, configFilePath)){
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(inputStream, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final QATestConfig qaTestConfig = jsonParser.readValueAs(QATestConfig.class);
				build.setQaTestConfig(qaTestConfig);
			}
		} catch (S3Exception e) {
			if (404 == e.statusCode()) {
				throw new ResourceNotFoundException("QA Configuration file is missing from the build '" + build.getId()+ "'.");
			} else {
				throw e;
			}
		}
	}

	@Override
	public void updateStatus(final Build build, final Build.Status newStatus) throws IOException {
		Build.Status origStatus = resolveOriginalStatus(build);
		if (origStatus != null) {
			validateStatusTransition(origStatus);
		}

		build.setStatus(newStatus);
		final String newStatusFilePath = pathHelper.getStatusFilePath(build, build.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);

		if (origStatus != null && origStatus != newStatus) {
			final String origStatusFilePath = pathHelper.getStatusFilePath(build, origStatus);
			s3Client.deleteObject(buildBucketName, origStatusFilePath);
			LOGGER.debug("Delete old status {} file and replace with {} in S3 for build id {}", origStatus.name(), newStatus.name(), build.getId());
		}
		sendStatusUpdateResponseMessage(build);
	}

	private Build.Status resolveOriginalStatus(final Build build) throws IOException {
		Build.Status currentStatus = build.getStatus();
		if (currentStatus == null) {
			return null;
		}

		if (statusFileExists(build, currentStatus)) {
			return currentStatus;
		}

		// Reload the latest persisted build status from storage. This may return null for
		// newly created builds which have not yet been fully written to S3.
		Build lastBuild = find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(),
				null, null, null, null);
		if (lastBuild != null && lastBuild.getStatus() != null) {
			return lastBuild.getStatus();
		}

		return currentStatus;
	}

	private boolean statusFileExists(final Build build, final Build.Status status) throws IOException {
		final String buildStatusPath = pathHelper.getStatusFilePath(build, status);
		try (InputStream inputStream = s3Client.getObject(buildBucketName, buildStatusPath)) {
			return inputStream != null;
		} catch (S3Exception e) {
			if (404 == e.statusCode()) {
				return false;
			}
			throw e;
		}
	}

	private void validateStatusTransition(final Build.Status currentStatus) {
		// All other terminal states remain immutable
		if (TERMINAL_STATUSES.contains(currentStatus)) {
			throw new IllegalStateException("Could not update build status as it has been already " + currentStatus.name());
		}
	}

	private void sendStatusUpdateResponseMessage(final Build build) {
		messagingHelper.sendResponse(buildStatusTextMessage,
				ImmutableMap.of("releaseCenterKey", build.getReleaseCenterKey(),
						"productKey", build.getProductKey(),
						"buildId", build.getId(),
						"buildStatus", build.getStatus().name()));
	}

	@Override
	public void addTag(Build build, Tag tag) {
		List<Tag> tags = build.getTags();
		if (CollectionUtils.isEmpty(tags)) {
			tags = new ArrayList<>();
		} else {
			String oldTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(Enum::name).collect(Collectors.joining(",")));
			s3Client.deleteObject(buildBucketName, oldTagFilePath);
		}
		tags.add(tag);
		build.setTags(tags);
		final String newTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(Enum::name).collect(Collectors.joining(",")));
		putFile(newTagFilePath, BLANK);
	}

	@Override
	public void saveTags(Build build, List<Tag> tags) {
		List<Tag> oldTags = build.getTags();
		if (!CollectionUtils.isEmpty(oldTags)) {
			String oldTagFilePath = pathHelper.getTagFilePath(build, oldTags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(Enum::name).collect(Collectors.joining(",")));
			s3Client.deleteObject(buildBucketName, oldTagFilePath);
		}
		build.setTags(tags);
		if (!CollectionUtils.isEmpty(tags)) {
			final String newTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(Enum::name).collect(Collectors.joining(",")));
			putFile(newTagFilePath, BLANK);
		}
	}

	@Override
	public void assertStatus(final Build build, final Build.Status ensureStatus) throws BadConfigurationException {
		if (build.getStatus() != ensureStatus) {
			throw new BadConfigurationException("Build " + build.getCreationTime() + " is at status: " + build.getStatus().name()
					+ " and is expected to be at status:" + ensureStatus.name());
		}
	}

	@Override
	public InputStream getOutputFileStream(final Build build, final String filePath) {
		final String outputFilePath = pathHelper.getOutputFilesPath(build) + filePath;
		return srsFileHelper.getFileStream(outputFilePath);
	}

	@Override
	public List<String> listInputFileNames(final Build build) {
		final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build.getReleaseCenterKey(), build.getProductKey(), build.getId()).toString();
		return srsFileHelper.listFiles(buildInputFilesPath);
	}

	@Override
	public InputStream getInputFileStream(final Build build, final String inputFile) {
		final String path = pathHelper.getBuildInputFilePath(build, inputFile);
		return srsFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getLocalInputFileStream(final Build build, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		return new FileInputStream(localFile);
	}

	@Override
	public AsyncPipedStreamBean getOutputFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String buildOutputFilePath = pathHelper.getBuildOutputFilePath(build, relativeFilePath);
		return getFileAsOutputStream(buildOutputFilePath);
	}

	@Override
	public AsyncPipedStreamBean getLogFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String buildLogFilePath = pathHelper.getBuildLogFilePath(build, relativeFilePath);
		return getFileAsOutputStream(buildLogFilePath);
	}

	@Override
	public void copyInputFileToOutputFile(final Build build, final String relativeFilePath) {
		final String buildInputFilePath = pathHelper.getBuildInputFilePath(build, relativeFilePath);
		final String buildOutputFilePath = pathHelper.getBuildOutputFilePath(build, relativeFilePath);
		srsFileHelper.copyFile(buildInputFilePath, buildOutputFilePath);
	}

	@Override
	public void copyBuildToAnother(String sourceBucketName,String sourceBuildPath, String destinationBucketName , String destBuildPath, String folder) {
		final String sourceFolder = sourceBuildPath + folder;
		final String destFolder = destBuildPath + folder;
		List<String> filenames = listFiles(sourceBucketName, sourceFolder);
		for (String filename : filenames) {
			this.s3Client.copyObject(sourceBucketName, sourceFolder + filename, destinationBucketName, destFolder + filename);
		}
	}

	@Override
	public InputStream getOutputFileInputStream(final Build build, final String name) {
		final String path = pathHelper.getBuildOutputFilePath(build, name);
		return srsFileHelper.getFileStream(path);
	}

	@Override
	public InputStream getOutputFileInputStream(String buildPath, String name) {
		return srsFileHelper.getFileStream(buildPath + S3PathHelper.OUTPUT_FILES + S3PathHelper.SEPARATOR + name);
	}

	@Override
	public InputStream getOutputFileInputStream(String bucketName, String buildPath, String name) {
		return this.s3Client.getObject(bucketName, buildPath + S3PathHelper.OUTPUT_FILES + S3PathHelper.SEPARATOR + name);
	}

	@Override
	public String putOutputFile(final Build build, final File file, final boolean calcMD5) throws IOException {
		final String filename = file.getName();
		final String outputFilePath = pathHelper.getBuildOutputFilePath(build, filename);
		try {
			return srsFileHelper.putFile(file, outputFilePath, calcMD5);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading " + filename, e);
		}
	}

	@Override
	public String putOutputFile(final Build build, final File file) throws IOException {
		return putOutputFile(build, file, false);
	}


	@Override
	public String putInputFile(final Build build, final File file, final boolean calcMD5) throws IOException {
		final String filename = file.getName();
		final String inputFilePath = pathHelper.getBuildInputFilePath(build, filename);
		try {
			return srsFileHelper.putFile(file, inputFilePath, calcMD5);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading " + filename, e);
		}
	}

	@Override
	public InputStream getManifestStream(final Build build) {
		String manifestFilePath = getManifestFilePath(build);
		if (manifestFilePath != null) {
			LOGGER.info("Opening manifest file found at {}", manifestFilePath);
			return srsFileHelper.getFileStream(manifestFilePath);
		} else {
			LOGGER.error("Failed to find manifest file for {}", build.getId());
			return null;
		}
	}

	@Override
	public List<String> listTransformedFilePaths(final Build build) {
		final String transformedFilesPath = pathHelper.getBuildTransformedFilesPath(build).toString();
		return srsFileHelper.listFiles(transformedFilesPath);
	}

	@Override
	public List<String> listOutputFilePaths(final Build build) {
		final String outputFilesPath = pathHelper.getOutputFilesPath(build);
		return srsFileHelper.listFiles(outputFilesPath);
	}

	@Override
	public List<String> listOutputFilePaths(String buildPath) {
		return srsFileHelper.listFiles(buildPath + S3PathHelper.OUTPUT_FILES + S3PathHelper.SEPARATOR);
	}

	@Override
	public List<String> listOutputFilePaths(String bucketName, String path) {
		return listFiles(bucketName, path);
	}

	@Override
	public List<String> listBuildLogFilePaths(final Build build) {
		final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
		return srsFileHelper.listFiles(logFilesPath);
	}

	@Override
	public InputStream getLogFileStream(final Build build, final String logFileName) {
		final String logFilePath = pathHelper.getBuildLogFilePath(build, logFileName);
		return srsFileHelper.getFileStream(logFilePath);
	}

	@Override
	public String getTelemetryBuildLogFilePath(final Build build) {
		final String buildLogFilePath = pathHelper.getMainBuildLogFilePath(build);
		return TelemetryStreamPathBuilder.getS3StreamDestinationPath(buildBucketName, buildLogFilePath);
	}

	@Override
	public AsyncPipedStreamBean getTransformedFileOutputStream(final Build build, final String relativeFilePath) throws IOException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		return getFileAsOutputStream(transformedFilePath);
	}

	@Override
	public OutputStream getLocalTransformedFileOutputStream(final Build build, final String relativeFilePath) throws FileNotFoundException {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		final File localFile = getLocalFile(transformedFilePath);
		localFile.getParentFile().mkdirs();
		return new FileOutputStream(localFile);
	}

	@Override
	public InputStream getTransformedFileAsInputStream(final Build build,
													   final String relativeFilePath) {
		final String transformedFilePath = pathHelper.getTransformedFilePath(build, relativeFilePath);
		return srsFileHelper.getFileStream(transformedFilePath);
	}

	@Override
	public InputStream getPublishedFileArchiveEntry(final String releaseCenterKey, final String targetFileName, final String previousPublishedPackage) {
		// For scenarios in UAT and DEV where we use locally published release packages for a new build,
		// if the file is not found in srs.published.releases.storage.path, then look in srs.publish.job.storage.path
		String publishedZipPath = pathHelper.getPublishedReleasesFilePath(releaseCenterKey, previousPublishedPackage);
		if (!srsFileHelper.exists(publishedZipPath) && !publishedReleasesStoragePath.equals(publishJobStoragePath)) {
			LOGGER.warn("Could not find previously published package {}", publishedZipPath);
			publishedZipPath = pathHelper.getPublishJobFilePath(releaseCenterKey, previousPublishedPackage);
		}
		final String publishedExtractedZipPath = publishedZipPath.replace(".zip", "/");
		LOGGER.debug("targetFileName: {}", targetFileName);
		String targetFileNameStripped = rf2FileNameTransformation.transformFilename(targetFileName);
		if (!Normalizer.isNormalized(targetFileNameStripped, Normalizer.Form.NFC)) {
			targetFileNameStripped = Normalizer.normalize(targetFileNameStripped, Normalizer.Form.NFC);
		}

		final List<String> filePaths = srsFileHelper.listFiles(publishedExtractedZipPath);
		for (final String filePath : filePaths) {
			String filename = FileUtils.getFilenameFromPath(filePath);
			// use contains rather that startsWith so that we can have candidate release (with x prefix in the filename)
			// as previous published release.
			if (!Normalizer.isNormalized(filename, Normalizer.Form.NFC)) {
				filename = Normalizer.normalize(filename, Normalizer.Form.NFC);
			}
			if (filename.contains(targetFileNameStripped)) {
				return srsFileHelper.getFileStream(publishedExtractedZipPath + filePath);
			}
		}
		if (filePaths.isEmpty()) {
			LOGGER.error("No files found in the previous published package {}", previousPublishedPackage);
		} else {
			LOGGER.warn("No file found in the previous published package {} containing {}", previousPublishedPackage, targetFileNameStripped);
		}
		return null;
	}

	@Override
	public void persistReport(final Build build) {
		String reportPath = pathHelper.getReportPath(build);
		// Get the build report as a string we can write to disk/S3 synchronously because it's small
		String buildReportJSON = build.getBuildReport().toString();
		try (InputStream is = IOUtils.toInputStream(buildReportJSON, StandardCharsets.UTF_8)) {
			srsFileHelper.putFile(is, buildReportJSON.length(), reportPath);
		} catch (final IOException e) {
			LOGGER.error("Unable to persist build report", e);
		}
    }

	@Override
	public void renameTransformedFile(final Build build, final String sourceFileName, final String targetFileName, boolean deleteOriginal) {
		final String sourceFilePath = pathHelper.getTransformedFilePath(build, sourceFileName);
		final String targetFilePath = pathHelper.getTransformedFilePath(build, targetFileName);
		srsFileHelper.copyFile(sourceFilePath, targetFilePath);
		if (deleteOriginal) {
			srsFileHelper.deleteFile(sourceFilePath);
		}
	}

	private List<Build> findBuildsDesc(final String directoryPathPrefix, BuildRequestParameter requestParameter) {

		// Not easy to make this efficient because our timestamp immediately under the product name means that we can only prefix
		// with the product name. The S3 API doesn't allow us to pattern match just the status files.
		// I think an "index" directory might be the solution

		// I think adding a pipe to the end of the status filename and using that as the delimiter would be
		// the simplest way to give performance - KK
		LOGGER.trace("Finding all Builds in {}, {}.", buildBucketName, directoryPathPrefix);
		BuildResponse response = getAllBuildsFromS3(directoryPathPrefix, requestParameter.releaseCenterKey, requestParameter.productKey, requestParameter.forYears);
		List<Build> builds = response.builds();
		Collections.reverse(builds);
		builds = removeInvisibleBuilds(requestParameter.visibility, response.visibilityPaths, builds);
		builds = removeBuildsMarkAsDeleted(response.buildsMarkAsDeleted, builds);
		addDataToBuilds(builds, response.userPaths, response.userRolesPaths, response.tagPaths, requestParameter);
		LOGGER.trace("{} Builds being returned to client.", builds.size());
		return builds;
	}

	private BuildPage<Build> findBuilds(final String productDirectoryPath, BuildRequestParameter requestParameter) {
		int pageNumber = requestParameter.pageRequest.getPageNumber();
		int pageSize = requestParameter.pageRequest.getPageSize();
		if (pageNumber < 0 || pageSize <= 0) {
			LOGGER.debug("No builds found from negative/blank page request.");
			return BuildPage.empty();
		}

		LOGGER.trace("Finding all builds in {}, {}.", buildBucketName, productDirectoryPath);
		BuildResponse response = getAllBuildsFromS3(productDirectoryPath, requestParameter.releaseCenterKey, requestParameter.productKey, requestParameter.forYears);

		List<Build> allBuilds = prepareAllBuilds(response, requestParameter);
		List<Build> pagedBuilds = buildPagedBuilds(response, requestParameter, allBuilds, pageNumber, pageSize);

		int totalPages = ListHelper.getTotalPages(allBuilds, pageSize);
		LOGGER.info("Returning {} builds to client from {}, {}. {} pages available.", pagedBuilds.size(), buildBucketName, productDirectoryPath, totalPages);
		return new BuildPage<>(allBuilds.size(), totalPages, pageNumber, pageSize, pagedBuilds);
	}

	private List<Build> prepareAllBuilds(final BuildResponse response, final BuildRequestParameter requestParameter) {
		List<Build> allBuilds = response.builds();
		allBuilds = filterByViewMode(allBuilds, response.tagPaths, requestParameter.viewMode);
		allBuilds = removeInvisibleBuilds(requestParameter.visibility, response.visibilityPaths, allBuilds);
		return removeBuildsMarkAsDeleted(response.buildsMarkAsDeleted, allBuilds);
	}

	private List<Build> buildPagedBuilds(final BuildResponse response,
										 final BuildRequestParameter requestParameter,
										 final List<Build> allBuilds,
										 final int pageNumber,
										 final int pageSize) {
		if (allBuilds.isEmpty()) {
			return new ArrayList<>();
		}

		if (Sort.unsorted().equals(requestParameter.pageRequest.getSort())) {
			return buildPagedBuildsWithoutSort(response, requestParameter, allBuilds, pageNumber, pageSize);
		}

		return buildPagedBuildsWithSort(response, requestParameter, allBuilds, pageNumber, pageSize);
	}

	private List<Build> buildPagedBuildsWithSort(final BuildResponse response,
												 final BuildRequestParameter requestParameter,
												 final List<Build> allBuilds,
												 final int pageNumber,
												 final int pageSize) {
		joinUsers(allBuilds, response.userPaths);
		if (requestParameter.pageRequest.getSort().getOrderFor(SORT_PROPERTY_BUILD_NAME) != null) {
			joinBuildConfigurations(allBuilds);
		}

		sortBuilds(requestParameter.pageRequest, allBuilds);
		List<Build> pagedBuilds = pageBuilds(allBuilds, pageNumber, pageSize);

		if (requestParameter.pageRequest.getSort().getOrderFor(SORT_PROPERTY_BUILD_NAME) == null
				&& Boolean.TRUE.equals(requestParameter.includeBuildConfiguration)) {
			joinBuildConfigurations(pagedBuilds);
		}
		if (Boolean.TRUE.equals(requestParameter.includeQAConfiguration)) {
			joinQAConfigurations(pagedBuilds);
		}
		if (Boolean.TRUE.equals(requestParameter.includeRvfURL)) {
			joinRvfUrls(pagedBuilds);
		}
		joinRoles(allBuilds, response.userRolesPaths);
		joinTags(allBuilds, response.tagPaths);

		return pagedBuilds;
	}

	private List<Build> buildPagedBuildsWithoutSort(final BuildResponse response,
													final BuildRequestParameter requestParameter,
													final List<Build> allBuilds,
													final int pageNumber,
													final int pageSize) {
		List<Build> reversedBuilds = new ArrayList<>(allBuilds);
		Collections.reverse(reversedBuilds);
		List<Build> pagedBuilds = pageBuilds(reversedBuilds, pageNumber, pageSize);
		addDataToBuilds(pagedBuilds, response.userPaths, response.userRolesPaths, response.tagPaths, requestParameter);
		return pagedBuilds;
	}

	private void sortBuilds(PageRequest pageRequest, List<Build> allBuilds) {
		Comparator<Build> comparator = Comparator.nullsLast(null);
		Sort sort = pageRequest.getSort();
		for (Sort.Order order : sort.toList()) {
			comparator = applySortOrder(comparator, order);
		}
		allBuilds.sort(comparator);
	}

	private Comparator<Build> applySortOrder(final Comparator<Build> comparator, final Sort.Order order) {
		Comparator<Build> updatedComparator = comparator;
		boolean descending = order.getDirection().isDescending();

		switch (order.getProperty()) {
			case SORT_PROPERTY_BUILD_NAME -> updatedComparator = updatedComparator.thenComparing(
					Build::getBuildName,
					descending ? Comparator.nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Comparator.naturalOrder()));
			case SORT_PROPERTY_CREATION_TIME -> updatedComparator = updatedComparator.thenComparing(
					Build::getCreationTime,
					descending ? Comparator.nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Comparator.naturalOrder()));
			case SORT_PROPERTY_STATUS -> updatedComparator = updatedComparator.thenComparing(
					Build::getStatus,
					descending ? Comparator.nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Comparator.naturalOrder()));
			case SORT_PROPERTY_BUILD_USER -> updatedComparator = updatedComparator.thenComparing(
					Build::getBuildUser,
					descending ? Comparator.nullsLast(Comparator.reverseOrder()) : Comparator.nullsLast(Comparator.naturalOrder()));
			default -> {
				// No-op for unsupported properties
			}
		}

		return updatedComparator;
	}

	private BuildResponse getAllBuildsFromS3(String directoryPathPrefix, String releaseCenterKey, String productKey, List<Integer> forYears) {
		LOGGER.trace("Reading Builds in {}, {} in batches.", buildBucketName, directoryPathPrefix);
        GetS3ObjectResponse getS3ObjectResponse = getS3Objects(buildBucketName, releaseCenterKey, productKey, directoryPathPrefix, forYears);
        BuildResponse response = findBuilds(releaseCenterKey, productKey, getS3ObjectResponse.s3Objects());
        if (Boolean.TRUE.equals(getS3ObjectResponse.isGetAllBuild())) {
            this.buildIdsToProductMap.put(releaseCenterKey + org.ihtsdo.otf.RF2Constants.DASH + productKey, response.builds.stream().map(Build::getId).collect(Collectors.toSet()));
        }
        LOGGER.trace("Found {} Builds in {}, {}.", response.builds.size(), buildBucketName, directoryPathPrefix);
        return response;
    }

	private List<Build> removeInvisibleBuilds(Boolean visibility, List<String> visibilityPaths, List<Build> builds) {
		LOGGER.trace("Removing invisible builds.");
		if (visibility != null && !visibilityPaths.isEmpty() && !builds.isEmpty()) {
			List<String> invisibleBuildIds = getInvisibleBuilds(visibilityPaths);
			if (visibility) {
				return builds.stream()
						.filter(build -> !invisibleBuildIds.contains(build.getCreationTime()))
						.toList();
			} else {
				return builds.stream()
						.filter(build -> invisibleBuildIds.contains(build.getCreationTime()))
						.toList();
			}
		}

		LOGGER.trace("{} Builds remaining.", builds.size());
		return builds;
	}

	private List<Build> removeBuildsMarkAsDeleted(List<String> buildsMarkAsDeleted, List<Build> builds) {
		if (!buildsMarkAsDeleted.isEmpty()) {
			return builds.stream()
					.filter(build -> !buildsMarkAsDeleted.contains(build.getCreationTime())).toList();
		}
		return builds;
	}

	private List<Build> filterByViewMode(List<Build> allBuilds, List<String> tagPaths, BuildService.View viewMode) {
		return switch (viewMode) {
			case PUBLISHED -> filterPublishedBuilds(allBuilds, tagPaths);
			case UNPUBLISHED -> filterUnpublishedBuilds(allBuilds, tagPaths);
			case ALL_RELEASES -> allBuilds;
			case DEFAULT -> filterDefaultViewBuilds(allBuilds, tagPaths);
			default -> allBuilds;
		};
	}

	private List<Build> filterPublishedBuilds(final List<Build> allBuilds, final List<String> tagPaths) {
		return allBuilds.stream()
				.filter(build -> getTags(build, tagPaths).contains(Tag.PUBLISHED))
				.toList();
	}

	private List<Build> filterUnpublishedBuilds(final List<Build> allBuilds, final List<String> tagPaths) {
		return allBuilds.stream()
				.filter(build -> !getTags(build, tagPaths).contains(Tag.PUBLISHED))
				.toList();
	}

	private List<Build> filterDefaultViewBuilds(final List<Build> allBuilds, final List<String> tagPaths) {
		List<Build> copy = new ArrayList<>(allBuilds);
		Collections.reverse(copy);
		Build latestPublishedBuild = null;
		List<Build> publishedBuilds = new ArrayList<>();
		for (Build build : copy) {
			if (getTags(build, tagPaths).contains(Tag.PUBLISHED)) {
				if (latestPublishedBuild == null) {
					latestPublishedBuild = build;
				} else {
					publishedBuilds.add(build);
				}
			}
		}
		if (latestPublishedBuild != null) {
			List<Build> result = new ArrayList<>(copy.subList(0, copy.indexOf(latestPublishedBuild) + 1));
			result.addAll(publishedBuilds);
			Collections.reverse(result);
			return result;
		}
		return allBuilds;
	}

	private List<Build> pageBuilds(List<Build> builds, int pageNumber, int pageSize) {
		LOGGER.trace("Fetching pageNumber {} with pageSize {} from {} builds.", pageNumber, pageSize, builds.size());
		return ListHelper.page(builds, pageNumber, pageSize);
	}

	private void joinUsers(List<Build> builds, List<String> userPaths) {
		builds.forEach(build -> build.setBuildUser(getBuildUser(build, userPaths)));
	}

	private void joinTags(List<Build> builds, List<String> tagPaths) {
		builds.forEach(build -> build.setTags(getTags(build, tagPaths)));
	}

	private void joinRoles(List<Build> builds, List<String> rolesPaths) {
		builds.forEach(build -> build.setUserRoles(getUserRoles(build, rolesPaths)));
	}

	private void joinBuildConfigurations(List<Build> builds) {
		builds.forEach(build -> {
			try {
				this.loadBuildConfiguration(build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving Build Configuration for build {}", build.getId());
			}
		});
	}

	private void joinQAConfigurations(List<Build> builds) {
		builds.forEach(build -> {
			try {
				this.loadQaTestConfig(build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving QA Configuration for build {}", build.getId());
			}
		});
	}

	private void joinRvfUrls(List<Build> builds) {
		builds.forEach(this::populateRvfUrlIfPresent);
	}

	private void addDataToBuilds(List<Build> builds, List<String> userPaths, List<String> userRolesPaths, List<String> tagPaths, BuildRequestParameter requestParameter) {
		LOGGER.trace("Adding users, tags & build reports to builds.");
		if (!builds.isEmpty()) {
			builds.forEach(build -> {
				enrichBuildWithUserAndTags(build, userPaths, userRolesPaths, tagPaths);
				enrichBuildWithRvfIfRequested(build, requestParameter);
				enrichBuildWithConfigurationsIfRequested(build, requestParameter);
			});
		}
	}

	private void enrichBuildWithUserAndTags(final Build build,
											final List<String> userPaths,
											final List<String> userRolesPaths,
											final List<String> tagPaths) {
		build.setBuildUser(getBuildUser(build, userPaths));
		build.setUserRoles(getUserRoles(build, userRolesPaths));
		build.setTags(getTags(build, tagPaths));
	}

	private void enrichBuildWithRvfIfRequested(final Build build, final BuildRequestParameter requestParameter) {
		if (Boolean.TRUE.equals(requestParameter.includeRvfURL)) {
			populateRvfUrlIfPresent(build);
		}
	}

	private void enrichBuildWithConfigurationsIfRequested(final Build build, final BuildRequestParameter requestParameter) {
		if (Boolean.TRUE.equals(requestParameter.includeBuildConfiguration)) {
			try {
				this.loadBuildConfiguration(build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving Build Configuration for build {}", build.getId());
			}
		}
		if (Boolean.TRUE.equals(requestParameter.includeQAConfiguration)) {
			try {
				this.loadQaTestConfig(build);
			} catch (IOException e) {
				LOGGER.error("Error retrieving QA Configuration for build {}", build.getId());
			}
		}
	}

	private boolean isRvfRelevantStatus(final Build.Status status) {
		return Build.Status.BUILT.equals(status)
				|| Build.Status.RVF_QUEUED.equals(status)
				|| Build.Status.RVF_RUNNING.equals(status)
				|| Build.Status.RELEASE_COMPLETE.equals(status)
				|| Build.Status.RELEASE_COMPLETE_WITH_WARNINGS.equals(status);
	}

	private void populateRvfUrlIfPresent(final Build build) {
		if (!isRvfRelevantStatus(build.getStatus())) {
			return;
		}

		InputStream buildReportStream = getBuildReportFileStream(build);
		if (buildReportStream == null) {
			return;
		}

		JSONParser jsonParser = new JSONParser();
		try {
			JSONObject jsonObject = (org.json.simple.JSONObject) jsonParser.parse(new InputStreamReader(buildReportStream, StandardCharsets.UTF_8));
			if (jsonObject.containsKey(RVF_RESPONSE_KEY)) {
				build.setRvfURL(jsonObject.get(RVF_RESPONSE_KEY).toString());
			}
		} catch (IOException e) {
			LOGGER.error("Error reading rvf_url from build_report file. Error: {}", e.getMessage());
		} catch (ParseException e) {
			LOGGER.error("Error parsing build_report file. Error: {}", e.getMessage());
		}
	}

	private BuildResponse findBuilds(final String releaseCenterKey, final String productKey, final List<S3Object> s3Objects) {
		List<Build> builds = new ArrayList<>();
		List<String> userPaths = new ArrayList<>();
		List<String> userRolesPaths = new ArrayList<>();
		List<String> tagPaths = new ArrayList<>();
		List<String> visibilityPaths = new ArrayList<>();
		List<String> buildsMarkAsDeleted = new ArrayList<>();
		for (final S3Object s3Object : s3Objects) {
			final String key = s3Object.key();
			if (key.contains("/status:")) {
				final String[] keyParts = key.split("/");
				final String dateString = keyParts[keyParts.length - 2];
				final String status = keyParts[keyParts.length - 1].split(":")[1];
				final Build build = new Build(dateString, releaseCenterKey, productKey, status);
				builds.add(build);
			} else if (key.contains("/tag:")) {
				tagPaths.add(key);
			} else if (key.contains("/user:")) {
				userPaths.add(key);
			} else if (key.contains("/user-roles:")) {
				userRolesPaths.add(key);
			} else if (key.contains("/visibility:")) {
				visibilityPaths.add(key);
			} else if (key.contains("/" + S3PathHelper.MARK_AS_DELETED)) {
				final String[] keyParts = key.split("/");
				final String dateString = keyParts[keyParts.length - 2];
				buildsMarkAsDeleted.add(dateString);
			}
		}

		builds.sort((Comparator.comparing(Build::getId)));
		return new BuildResponse(builds, userPaths, userRolesPaths, tagPaths, visibilityPaths, buildsMarkAsDeleted);
	}

	private List<String> getUserRoles(Build build, final List<String> userRolesPaths) {
		for (final String key : userRolesPaths) {
			final String[] keyParts = key.split("/");
			final String dateString = keyParts[keyParts.length - 2];
			if (build.getCreationTime().equals(dateString)) {
				String rolesStr = keyParts[keyParts.length - 1].split(":")[1];
				String[] roleArr = rolesStr.split(",");
				return Arrays.asList(roleArr);
			}
		}
		return Collections.emptyList();
	}

	private List<Tag> getTags(Build build, final List<String> tagPaths) {
		for (final String key : tagPaths) {
			final String[] keyParts = key.split("/");
			final String dateString = keyParts[keyParts.length - 2];
			if (build.getCreationTime().equals(dateString)) {
				String tagsStr = keyParts[keyParts.length - 1].split(":")[1];
				String[] tagArr = tagsStr.split(",");
				List<Tag> tags = new ArrayList<>();
				for (String tag : tagArr) {
					tags.add(Build.Tag.valueOf(tag));
				}
				return tags;
			}
		}
		return Collections.emptyList();
	}

	private String getBuildUser(Build build, final List<String> userPaths) {
		for (final String key : userPaths) {
			final String[] keyParts = key.split("/");
			final String dateString = keyParts[keyParts.length - 2];
			if (build.getCreationTime().equals(dateString)) {
				return keyParts[keyParts.length - 1].split(":")[1];
			}
		}
		return null;
	}

	private  List<String> getInvisibleBuilds(final List<String> visibilityPaths) {
		List<String> result = new ArrayList<>();
		for (final String key : visibilityPaths) {
			final String[] keyParts = key.split("/");
			final String dateString = keyParts[keyParts.length - 2];
			final boolean visibility = Boolean.parseBoolean(keyParts[keyParts.length - 1].split(":")[1]);
			if (!visibility) {
				result.add(dateString);
			}
		}

		return result;
	}

	private AsyncPipedStreamBean getFileAsOutputStream(final String buildOutputFilePath) throws IOException {
        // Stream file to buildFileHelper as it's written to the OutputStream
        // try with resource here won't work
        final PipedInputStream pipedInputStream = new PipedInputStream();
        final PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

        final Future<String> future = executorService.submit(() -> {
            try {
                srsFileHelper.putFile(pipedInputStream, buildOutputFilePath);
                LOGGER.debug("Build output file stream ended: {}", buildOutputFilePath);
                return buildOutputFilePath;
            } finally {
                try {
                    pipedInputStream.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing pipedInputStream for {}", buildOutputFilePath, e);
                }
            }
        });

        return new AsyncPipedStreamBean(outputStream, future, buildOutputFilePath);
	}

	private PutObjectResponse putFile(final String filePath, final String contents) {
		return s3Client.putObject(buildBucketName, filePath, contents.getBytes(StandardCharsets.UTF_8));
	}

	private File getLocalFile(final String transformedFilePath) {
		return new File(tempDir, transformedFilePath);
	}

	@Override
	public void loadConfiguration(final Build build) throws IOException {
		loadBuildConfiguration(build);
		loadQaTestConfig(build);
	}

	@Override
	public String getOutputFilePath(Build build, String filename) {
		return pathHelper.getOutputFilesPath(build) + filename;
	}

	@Override
	public String getManifestFilePath(Build build) {
		final String directoryPath = pathHelper.getBuildManifestDirectoryPath(build);
		final List<String> files = srsFileHelper.listFiles(directoryPath);
		//The first file in the manifest directory we'll call our manifest
		if (!files.isEmpty()) {
			String fileName = files.stream()
					.filter(file -> file != null && file.endsWith(".xml"))
					.findAny()
					.orElse(null);
			if (fileName != null) {
				final String manifestFilePath = directoryPath + fileName;
				LOGGER.info("manifest file found at {}", manifestFilePath);
				return manifestFilePath;
			}
		}
		return null;
	}

	@Override
	public InputStream getBuildReportFileStream(Build build) {
		final String reportFilePath = pathHelper.getReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	@Override
	public InputStream getBuildInputFilesPrepareReportStream(Build build) {
		final String reportFilePath = pathHelper.getBuildInputFilePrepareReportPath(build.getReleaseCenterKey(), build.getProductKey(), build.getId());
		return srsFileHelper.getFileStream(reportFilePath);
	}

	@Override
	public boolean isBuildCancelRequested(final Build build) {
		if (Build.Status.CANCEL_REQUESTED.equals(build.getStatus())) return true;
		String cancelledRequestedPath = pathHelper.getStatusFilePath(build, Build.Status.CANCEL_REQUESTED);
		try (final InputStream inputStream = s3Client.getObject(buildBucketName, cancelledRequestedPath)){
			if (inputStream != null) {
				build.setStatus(Build.Status.CANCEL_REQUESTED);
				LOGGER.info("Build status is {}. Build will be cancelled when possible", build.getStatus().name());
				return true;
			}
		} catch (IOException e) {
            LOGGER.error("Error when reading build status for {}", build.getId(), e);
        } catch (S3Exception e) {
            // Doesn't exist if not cancelled
        }
		return false;
	}

	@Override
	public void deleteOutputFiles(Build build) {
		List<String> outputFiles = listOutputFilePaths(build);
		final String outputFilesPath = pathHelper.getOutputFilesPath(build);
		for (String outputFile : outputFiles) {
			// S3 deletes are idempotent; avoid an extra existence check per object.
			srsFileHelper.deleteFile(outputFilesPath + outputFile);
		}
	}

	@Override
	public void deleteTransformedFiles(Build build) {
		List<String> transformedFiles = listTransformedFilePaths(build);
		final String transformedFilesPath = pathHelper.getBuildTransformedFilesPath(build).toString();
		for (String transformedFile : transformedFiles) {
			// S3 deletes are idempotent; avoid an extra existence check per object.
			srsFileHelper.deleteFile(transformedFilesPath + transformedFile);
		}
	}

	@Override
	public void cleanupForRetry(Build build) {
		/*
		 * Delete partial artifacts under the build folder so the build can be retried, while preserving:
		 * - Everything under manifest/
		 * - Non-JSON files at the build root
		 * - The two configuration JSONs at the build root
		 */
		final String buildPrefix = ensureTrailingSlash(pathHelper.getBuildPath(build).toString());
		final List<String> files = srsFileHelper.listFiles(buildPrefix);
		if (files.isEmpty()) {
			return;
		}

		int deleted = 0;
		for (String relativePathRaw : files) {
			final String relativePath = normalizeRelativeKey(relativePathRaw);
			final boolean shouldDelete = relativePath != null && shouldDeleteForRetry(relativePath);
			if (shouldDelete) {
				// S3 deletes are idempotent; avoid an extra existence check per object.
				srsFileHelper.deleteFile(buildPrefix + relativePath);
				deleted++;
			}
		}
		LOGGER.debug("Cleaned up {} objects under build {} for retry.", deleted, build.getId());
	}


	private static String ensureTrailingSlash(String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return S3PathHelper.SEPARATOR;
		}
		return prefix.endsWith(S3PathHelper.SEPARATOR) ? prefix : prefix + S3PathHelper.SEPARATOR;
	}

	private static String normalizeRelativeKey(String relativePath) {
		if (relativePath == null) {
			return null;
		}
		final String trimmed = relativePath.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		// Defensive: some callers may include a leading separator; FileHelper.listFiles normally does not.
		return trimmed.startsWith(S3PathHelper.SEPARATOR) ? trimmed.substring(1) : trimmed;
	}

	private static boolean shouldDeleteForRetry(String relativePath) {
		// Preserve manifest directory contents (and potential directory marker key)
		if (relativePath.equals("manifest") || relativePath.startsWith(MANIFEST_DIR_PREFIX)) {
			return false;
		}

		final boolean isInSubfolder = relativePath.contains(S3PathHelper.SEPARATOR);
		if (isInSubfolder) {
			// Delete all files within subfolders (except manifest/* handled above)
			return true;
		}

		// Root-level file: delete only JSON files except config jsons
		if (!relativePath.endsWith(".json")) {
			return false;
		}
		return !(S3PathHelper.CONFIG_JSON.equals(relativePath) || S3PathHelper.QA_CONFIG_JSON.equals(relativePath));
	}

	@Override
	public InputStream getBuildInputGatherReportStream(Build build) {
		String reportFilePath = pathHelper.getBuildInputGatherReportPath(build.getReleaseCenterKey(), build.getProductKey(), build.getId());
		return srsFileHelper.getFileStream(reportFilePath);
	}


	@Override
	public boolean isDerivativeProduct(Build build) {
		ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
		if (extensionConfig == null) {
			return false;
		}
		return INTERNATIONAL.equalsIgnoreCase(build.getReleaseCenterKey()) && StringUtils.isNotBlank(extensionConfig.getDependencyRelease());

	}

	@Override
	public void updatePreConditionCheckReport(final Build build) throws IOException {
		File preConditionChecksReport = null;
		try {
			preConditionChecksReport = toJson(build.getPreConditionCheckReports());
			try (InputStream reportInputStream = new FileInputStream(preConditionChecksReport)) {
                s3Client.putObject(buildBucketName, pathHelper.getBuildPreConditionCheckReportPath(build), reportInputStream, ObjectMetadata.builder().build(), preConditionChecksReport.length());
            }
		} finally {
			if (preConditionChecksReport != null) {
                Files.deleteIfExists(preConditionChecksReport.toPath());
			}
		}
	}

	@Override
	public void updatePostConditionCheckReport(final Build build, final Object object) throws IOException {
		File postConditionChecksReport = null;
		try {
			postConditionChecksReport = toJson(object);
			try (InputStream reportInputStream = new FileInputStream(postConditionChecksReport)) {
                s3Client.putObject(buildBucketName, pathHelper.getPostConditionCheckReportPath(build), reportInputStream, ObjectMetadata.builder().build(), postConditionChecksReport.length());
            }
		} finally {
			if (postConditionChecksReport != null) {
                Files.deleteIfExists(postConditionChecksReport.toPath());
			}
		}
	}

	@Override
	public InputStream getPreConditionCheckReportStream(final Build build) {
		final String reportFilePath = pathHelper.getBuildPreConditionCheckReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	@Override
	public List<PreConditionCheckReport> getPreConditionCheckReport(final Build build) throws IOException {
		final String reportFilePath = pathHelper.getBuildPreConditionCheckReportPath(build);
		return getPreConditionCheckReport(reportFilePath);
	}

	@Override
	public List<PreConditionCheckReport> getPreConditionCheckReport(String reportPath) throws IOException {
        return getPreConditionCheckReport(buildBucketName, reportPath);
	}

	@Override
	public List<PreConditionCheckReport> getPreConditionCheckReport(String bucketName, String reportPath) throws IOException {
		List<PreConditionCheckReport> reports = new ArrayList<>();
		try (final InputStream s3Object = s3Client.getObject(bucketName, reportPath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));// Closes stream
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					reports = jsonParser.readValueAs(new TypeReference <List <PreConditionCheckReport>>() {
					});
				}
			}
		}

		return reports;
	}

	@Override
	public InputStream getPostConditionCheckReportStream(final Build build) {
		final String reportFilePath = pathHelper.getPostConditionCheckReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	@Override
	public List<PostConditionCheckReport> getPostConditionCheckReport(final Build build) throws IOException {
		final String reportFilePath = pathHelper.getPostConditionCheckReportPath(build);
		return getPostConditionCheckReport(reportFilePath);
	}

	@Override
	public List<PostConditionCheckReport> getPostConditionCheckReport(String reportPath) throws IOException {
        return getPostConditionCheckReport(buildBucketName, reportPath);
	}

	@Override
	public List<PostConditionCheckReport> getPostConditionCheckReport(String bucketName, String reportPath) throws IOException {
		List<PostConditionCheckReport> reports = new ArrayList<>();
		try (final InputStream s3Object = s3Client.getObject(bucketName, reportPath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));// Closes stream
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					reports = jsonParser.readValueAs(new TypeReference <List <PostConditionCheckReport>>() {
					});
				}
			}
		}

		return reports;
	}

	@Override
	public List<String> listClassificationResultOutputFileNames(Build build) {
		final String buildInputFilesPath = pathHelper.getClassificationResultOutputFilePath(build).toString();
		return srsFileHelper.listFiles(buildInputFilesPath);
	}

	@Override
	public String putClassificationResultOutputFile(final Build build, final File file) throws IOException {
		final String filename = file.getName();
		final String outputFilePath = pathHelper.getClassificationResultOutputPath(build, filename);
		try {
			return srsFileHelper.putFile(file, outputFilePath, false);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading " + filename, e);
		}
	}

	@Override
	public InputStream getClassificationResultOutputFileStream(Build build, String relativeFilePath) {
		final String path = pathHelper.getClassificationResultOutputPath(build, relativeFilePath);
		return srsFileHelper.getFileStream(path);
	}

	@Override
	public void updateVisibility(Build build, boolean visibility) throws IOException {
		// Deleting old regardless the visibility is true or false, or not being set yet
		String origStatusFilePath = pathHelper.getVisibilityFilePath(build, true);
		s3Client.deleteObject(buildBucketName, origStatusFilePath);
		origStatusFilePath = pathHelper.getVisibilityFilePath(build, false);
		s3Client.deleteObject(buildBucketName, origStatusFilePath);

		// Put new visibility
		final String newStatusFilePath = pathHelper.getVisibilityFilePath(build, visibility);
		putFile(newStatusFilePath, BLANK);
	}

	@Override
	public void putManifestFile(Build build, InputStream inputStream) throws IOException {
		final String filePath = pathHelper.getBuildManifestDirectoryPath(build);
		srsFileHelper.putFile(inputStream, filePath + "manifest.xml");
	}

	@Override
	public void saveBuildComparisonReport(String releaseCenterKey, String productKey, String compareId, BuildComparisonReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			s3Client.putObject(buildBucketName, pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId), reportInputStream, ObjectMetadata.builder().build(), reportFile.length());
		} finally {
			if (reportFile != null) {
                // remove local file
                Files.deleteIfExists(reportFile.toPath());
			}
		}
	}

	@Override
	public List<String> listBuildComparisonReportPaths(String releaseCenterKey, String productKey) {
		final String reportPath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, null);
		return srsFileHelper.listFiles(reportPath);
	}

	@Override
	public BuildComparisonReport getBuildComparisonReport(String releaseCenterKey, String productKey, String compareId) throws IOException {
		BuildComparisonReport report = null;
		String filePath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		try (final InputStream s3Object = s3Client.getObject(buildBucketName, filePath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));// Closes stream
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					report = jsonParser.readValueAs(BuildComparisonReport.class);
				}
			}
		}

		return report;
	}

	@Override
	public void deleteBuildComparisonReport(String releaseCenterKey, String productKey, String compareId) {
		String filePath = pathHelper.getBuildComparisonReportPath(releaseCenterKey, productKey, compareId);
		s3Client.deleteObject(buildBucketName, filePath);
	}

	@Override
	public void saveFileComparisonReport(String releaseCenterKey, String productKey, String compareId, boolean ignoreIdComparison, FileDiffReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			String reportFileName = report.getFileName().replace(".txt", ".diff.json") + "-" + ignoreIdComparison;
			s3Client.putObject(buildBucketName, pathHelper.getFileComparisonReportPath(releaseCenterKey, productKey, compareId, reportFileName), reportInputStream, ObjectMetadata.builder().build(), reportFile.length());
		} finally {
			if (reportFile != null) {
                Files.deleteIfExists(reportFile.toPath());
			}
		}
	}

	@Override
	public FileDiffReport getFileComparisonReport(String releaseCenterKey, String productKey, String compareId, String fileName, boolean ignoreIdComparison) throws IOException {
		FileDiffReport report = null;
		String reportFileName = fileName.replace(".txt", ".diff.json") + "-" + ignoreIdComparison;
		String filePath = pathHelper.getFileComparisonReportPath(releaseCenterKey, productKey, compareId, reportFileName);
		try (final InputStream s3Object = s3Client.getObject(buildBucketName, filePath)) {
			if (s3Object != null) {
				final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(s3Object, RF2Constants.UTF_8));// Closes stream
				try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
					report = jsonParser.readValueAs(FileDiffReport.class);
				}
			}
		}

		return report;
	}

	private List<String> listFiles(String bucketName, String path) {
		List<String> files = new ArrayList<>();

		try {
			ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(bucketName).prefix(path).maxKeys(10000).build();
			boolean done = false;
			while (!done) {
				ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
				for (S3Object s3Object : listObjectsResponse.contents()) {
					files.add(s3Object.key().substring(path.length()));
				}
				if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
					String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
					listObjectsRequest = ListObjectsRequest.builder().bucket(bucketName).prefix(path).maxKeys(10000).marker(nextMarker).build();
				} else {
					done = true;
				}
			}
		} catch (S3Exception e) {
			LOGGER.info("Probable attempt to get listing on non-existent directory: {} error {}", path, e.getLocalizedMessage());
		}
		return files;
	}

	private GetS3ObjectResponse getS3Objects(String buildBucketName, String releaseCenterKey, String productKey, String prefix, List<Integer> forYears) {
		final List<S3Object> s3Objects;
		boolean isGetAllBuilds = false;
		boolean isGetBuildsForProduct = prefix.endsWith(productKey) || prefix.endsWith(productKey + S3PathHelper.SEPARATOR);
		String key = releaseCenterKey + org.ihtsdo.otf.RF2Constants.DASH + productKey;
		if (isGetBuildsForProduct && buildIdsToProductMap.containsKey(key) && !buildIdsToProductMap.get(key).isEmpty()) {
			Set<String> buildIds = buildIdsToProductMap.get(key);
			if (!CollectionUtils.isEmpty(forYears)) {
				buildIds = buildIds.stream() .filter(item -> forYears.stream().anyMatch(year -> item.startsWith(String.valueOf(year)))).collect(Collectors.toSet());
			}
			s3Objects = new ArrayList<>();
			buildIds.stream().parallel().forEach(buildId ->
				s3Objects.addAll(doGetS3ObjectsByBuildId(buildBucketName, prefix, buildId))
			);
		} else {
			if (isGetBuildsForProduct && !CollectionUtils.isEmpty(forYears)) {
				s3Objects = new ArrayList<>();
				forYears.stream().parallel().forEach(year ->
					s3Objects.addAll(doGetS3Objects(buildBucketName, prefix + year))
				);
			} else {
				if (isGetBuildsForProduct) isGetAllBuilds = true;
				s3Objects = doGetS3Objects(buildBucketName, prefix);
			}
		}
		return new GetS3ObjectResponse(s3Objects.stream().filter(s3Object -> requiredFileExtensions.stream().anyMatch(item -> s3Object.key().contains(item))).toList(), isGetAllBuilds);
	}

	private List<S3Object> doGetS3Objects(String buildBucketName, String prefix) {
		List<S3Object> s3Objects = new ArrayList<>();
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix).maxKeys(10000).build();
		boolean done = false;
		while (!done) {
			ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
			s3Objects.addAll(listObjectsResponse.contents());
			if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
				String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
				listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix).maxKeys(10000).marker(nextMarker).build();
			} else {
				done = true;
			}
		}
		return s3Objects;
	}

	private List<S3Object> doGetS3ObjectsByBuildId(String buildBucketName, String prefix, String buildId) {
		List<S3Object> s3Objects = new ArrayList<>();
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix + buildId + S3PathHelper.SEPARATOR).delimiter("/").maxKeys(10000).build();
		boolean done = false;
		while (!done) {
			ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
			s3Objects.addAll(listObjectsResponse.contents());
			if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
				String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
				listObjectsRequest = ListObjectsRequest.builder().bucket(buildBucketName).prefix(prefix + buildId + S3PathHelper.SEPARATOR).delimiter("/").maxKeys(10000).marker(nextMarker).build();
			} else {
				done = true;
			}
		}
		return s3Objects;
	}
}
