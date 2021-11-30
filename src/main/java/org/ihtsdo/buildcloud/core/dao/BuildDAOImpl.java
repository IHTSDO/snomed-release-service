package org.ihtsdo.buildcloud.core.dao;

import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.dao.helper.ListHelper;
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
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
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
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
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

	@Autowired
	private ObjectMapper objectMapper;

	private final File tempDir;

	private final Rf2FileNameTransformation rf2FileNameTransformation;

	private S3Client s3Client;

	@Autowired
	private MessagingHelper messagingHelper;

	@Autowired
	private ActiveMQTextMessage buildStatusTextMessage;

	@Autowired
	private S3PathHelper pathHelper;

	private String buildBucketName;

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	public BuildDAOImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
			final S3Client s3Client,
			final S3ClientHelper s3ClientHelper) {
		executorService = Executors.newCachedThreadPool();
		buildBucketName = storageBucketName;
		srsFileHelper = new FileHelper(storageBucketName, s3Client, s3ClientHelper);
		this.tempDir = Files.createTempDir();
		rf2FileNameTransformation = new Rf2FileNameTransformation();
		this.s3Client = s3Client;
	}

	@Override
	public void copyManifestFileFromProduct(Build build) {
		// Copy manifest file
		final String manifestPath = inputFileDAO.getManifestPath(build.getProduct());
		if (manifestPath != null) {
			final String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
			final String manifestFileName = Paths.get(manifestPath).getFileName().toString();
			srsFileHelper.copyFile(manifestPath, buildManifestDirectoryPath + manifestFileName);
		}
	}

	@Override
	public void save(final Build build) throws IOException {
		// Save config file
		LOGGER.debug("Saving build {} for product {}", build.getId(), build.getProduct().getBusinessKey());
		File configJson = toJson(build.getConfiguration());
		File qaConfigJson = toJson(build.getQaTestConfig());
		try (FileInputStream buildConfigInputStream = new FileInputStream(configJson);
			 FileInputStream qaConfigInputStream = new FileInputStream(qaConfigJson)) {
			s3Client.putObject(buildBucketName, pathHelper.getBuildConfigFilePath(build), buildConfigInputStream, new ObjectMetadata());
			s3Client.putObject(buildBucketName, pathHelper.getQATestConfigFilePath(build), qaConfigInputStream, new ObjectMetadata());
		} finally {
			if (configJson != null) {
				configJson.delete();
			}
			if (qaConfigJson != null) {
				qaConfigJson.delete();
			}
		}

		// Save trigger user
		if (StringUtils.isNotEmpty(build.getBuildUser())) {
			final String userFilePath = pathHelper.getBuildUserFilePath(build, build.getBuildUser());
			// Put new status before deleting old to avoid there being none.
			putFile(userFilePath, BLANK);
		}

		// save build status
		final String newStatusFilePath = pathHelper.getStatusFilePath(build, build.getStatus());
		// Put new status before deleting old to avoid there being none.
		putFile(newStatusFilePath, BLANK);
		LOGGER.debug("Saved build {}", build.getId());
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
	public List<Build> findAllDesc(final Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) {
		final String productDirectoryPath = pathHelper.getProductPath(product).toString();
		return findBuildsDesc(productDirectoryPath, product, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility);
	}

	@Override
	public BuildPage<Build> findAllDescPage(Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility, BuildService.View viewMode, PageRequest pageRequest) {
		final String productDirectoryPath = pathHelper.getProductPath(product).toString();
		return findBuildsDescPage(productDirectoryPath, product, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility, viewMode, pageRequest);
	}

	@Override
	public Build find(final Product product, final String buildId, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) {
		final String buildDirectoryPath = pathHelper.getBuildPath(product, buildId).toString();
		final List<Build> builds = findBuildsDesc(buildDirectoryPath, product, includeBuildConfiguration, includeQAConfiguration, includeRvfURL, visibility);
		if (!builds.isEmpty()) {
			return builds.get(0);
		} else {
			return null;
		}
	}

	@Override
	public void delete(Product product, String buildId) {
		String buildDirectoryPath = pathHelper.getBuildPath(product, buildId).toString();
		for (S3ObjectSummary file : s3Client.listObjects(buildBucketName, buildDirectoryPath).getObjectSummaries()) {
			s3Client.deleteObject(buildBucketName, file.getKey());
		}
	}

	@Override
	public void loadBuildConfiguration(final Build build) throws IOException {
		final String configFilePath = pathHelper.getBuildConfigFilePath(build);
		try {
			final S3Object s3Object = s3Client.getObject(buildBucketName, configFilePath);
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final BuildConfiguration buildConfiguration = jsonParser.readValueAs(BuildConfiguration.class);
				build.setConfiguration(buildConfiguration);
			}
		} catch (AmazonS3Exception e) {
			if (404 == e.getStatusCode()) {
				throw new ResourceNotFoundException("Configuration file for build '" + build.getId()+ "' does not exist.");
			} else {
				throw e;
			}
		}
	}


	@Override
	public void loadQaTestConfig(final Build build) throws IOException {
		final String configFilePath = pathHelper.getQATestConfigFilePath(build);
		try {
			final S3Object s3Object = s3Client.getObject(buildBucketName, configFilePath);
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(configurationJson)) {
				final QATestConfig qaTestConfig = jsonParser.readValueAs(QATestConfig.class);
				build.setQaTestConfig(qaTestConfig);
			}

		} catch (AmazonS3Exception e) {
			if (404 == e.getStatusCode()) {
				throw new ResourceNotFoundException("QA Configuration file for build '" + build.getId()+ "' does not exist.");
			} else {
				throw e;
			}
		}
	}

	@Override
	public void updateStatus(final Build build, final Build.Status newStatus) {
		final Build.Status origStatus = build.getStatus();
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

	private void sendStatusUpdateResponseMessage(final Build build) {
		final Product product = build.getProduct();
		messagingHelper.sendResponse(buildStatusTextMessage,
				ImmutableMap.of("releaseCenterKey", product.getReleaseCenter().getBusinessKey(),
						"productKey", product.getBusinessKey(),
						"buildId", build.getId(),
						"buildStatus", build.getStatus().name()));
	}

	@Override
	public void addTag(Build build, Tag tag) {
		List<Tag> tags = build.getTags();
		if (CollectionUtils.isEmpty(tags)) {
			tags = new ArrayList<>();
		} else {
			String oldTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(t -> t.name()).collect(Collectors.joining(",")));
			s3Client.deleteObject(buildBucketName, oldTagFilePath);
		}
		tags.add(tag);
		build.setTags(tags);
		final String newTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(t -> t.name()).collect(Collectors.joining(",")));
		putFile(newTagFilePath, BLANK);
	}

	@Override
	public void saveTags(Build build, List<Tag> tags) {
		List<Tag> oldTags = build.getTags();
		if (!CollectionUtils.isEmpty(oldTags)) {
			String oldTagFilePath = pathHelper.getTagFilePath(build, oldTags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(t -> t.name()).collect(Collectors.joining(",")));
			s3Client.deleteObject(buildBucketName, oldTagFilePath);
		}
		build.setTags(tags);
		if (!CollectionUtils.isEmpty(tags)) {
			final String newTagFilePath = pathHelper.getTagFilePath(build, tags.stream().sorted(Comparator.comparingInt(Tag::getOrder)).map(t -> t.name()).collect(Collectors.joining(",")));
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
		final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build).toString();
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
	public void copyBuildToAnother(Build sourceBuild, Build destBuild, String folder) {
		final String sourceFolder = pathHelper.getBuildPath(sourceBuild).toString() + folder;
		final String destFolder = pathHelper.getBuildPath(destBuild).toString() + folder;

		final List<String> buildInputFilePaths = srsFileHelper.listFiles(sourceFolder);
		for (final String path : buildInputFilePaths) {
			srsFileHelper.copyFile(sourceFolder + path, destFolder + path);
		}
	}

	@Override
	public InputStream getOutputFileInputStream(final Build build, final String name) {
		final String path = pathHelper.getBuildOutputFilePath(build, name);
		return srsFileHelper.getFileStream(path);
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
	public void putTransformedFile(final Build build, final File file) throws IOException {
		final String name = file.getName();
		final String outputPath = pathHelper.getBuildTransformedFilesPath(build).append(name).toString();
		try {
			srsFileHelper.putFile(file, outputPath);
		} catch (NoSuchAlgorithmException | DecoderException e) {
			throw new IOException("Problem creating checksum while uploading transformed file " + name, e);
		}
	}

	@Override
	public InputStream getManifestStream(final Build build) {
		String manifestFilePath = getManifestFilePath(build);
		if (manifestFilePath != null) {
			LOGGER.info("Opening manifest file found at " + manifestFilePath);
			return srsFileHelper.getFileStream(manifestFilePath);
		} else {
			LOGGER.error("Failed to find manifest file for " + build.getId());
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
	public List<String> listLogFilePaths(final Build build) {
		final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
		return srsFileHelper.listFiles(logFilesPath);
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
	public InputStream getBuildLogFileStream(final Build build, final String logFileName) {
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
	public InputStream getPublishedFileArchiveEntry(final ReleaseCenter releaseCenter, final String targetFileName, final String previousPublishedPackage) throws IOException {
		final String publishedZipPath = pathHelper.getPublishedFilePath(releaseCenter, previousPublishedPackage);
		final String publishedExtractedZipPath = publishedZipPath.replace(".zip", "/");
		LOGGER.debug("targetFileName:" + targetFileName);
		String targetFileNameStripped = targetFileName;
		if (!Normalizer.isNormalized(targetFileNameStripped, Normalizer.Form.NFC)) {
			targetFileNameStripped = Normalizer.normalize(targetFileNameStripped, Normalizer.Form.NFC);
		}
		targetFileNameStripped = rf2FileNameTransformation.transformFilename(targetFileName);

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
			LOGGER.error("No files found in the previous published package", previousPublishedPackage);
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
		try (InputStream is = IOUtils.toInputStream(buildReportJSON, "UTF-8")) {
			srsFileHelper.putFile(is, buildReportJSON.length(), reportPath);
		} catch (final IOException e) {
			LOGGER.error("Unable to persist build report", e);
		}
	}

	@Override
	public void renameTransformedFile(final Build build, final String sourceFileName, final String targetFileName, boolean deleteOriginal) {
		final String soureFilePath = pathHelper.getTransformedFilePath(build, sourceFileName);
		final String targetFilePath = pathHelper.getTransformedFilePath(build, targetFileName);
		srsFileHelper.copyFile(soureFilePath, targetFilePath);
		if (deleteOriginal) {
			srsFileHelper.deleteFile(soureFilePath);
		}
	}

	private List<Build> findBuildsDesc(final String productDirectoryPath, final Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL, Boolean visibility) {
		final List<String> userPaths = new ArrayList<>();
		final List<String> tagPaths = new ArrayList<>();
		final List<String> visibilityPaths = new ArrayList<>();

		// Not easy to make this efficient because our timestamp immediately under the product name means that we can only prefix
		// with the product name. The S3 API doesn't allow us to pattern match just the status files.
		// I think an "index" directory might be the solution

		// I think adding a pipe to the end of the status filename and using that as the delimiter would be
		// the simplest way to give performance - KK
		LOGGER.info("Finding all Builds in {}, {}.", buildBucketName, productDirectoryPath);
		List<Build> builds = getAllBuildsFromS3(productDirectoryPath, product, userPaths, tagPaths, visibilityPaths);
		builds = removeInvisibleBuilds(visibility, visibilityPaths, builds);
		addDataToBuilds(builds, userPaths, tagPaths, includeBuildConfiguration, includeQAConfiguration, includeRvfURL);
		Collections.reverse(builds);

		LOGGER.info("{} Builds being returned to client.", builds.size());
		return builds;
	}

	private BuildPage<Build> findBuildsDescPage(final String productDirectoryPath, final Product product, Boolean includeBuildConfiguration, Boolean includeQAConfiguration,
												Boolean includeRvfURL, Boolean visibility, BuildService.View viewMode, PageRequest pageRequest) {
		int pageNumber = pageRequest.getPageNumber();
		int pageSize = pageRequest.getPageSize();
		if (pageNumber < 0 || pageSize <= 0) {
			LOGGER.debug("No builds found from negative/blank page request.");
			return BuildPage.empty();
		}

		final List<String> userPaths = new ArrayList<>();
		final List<String> tagPaths = new ArrayList<>();
		final List<String> visibilityPaths = new ArrayList<>();

		LOGGER.info("Finding all Builds in {}, {}.", buildBucketName, productDirectoryPath);
		List<Build> allBuilds = getAllBuildsFromS3(productDirectoryPath, product, userPaths, tagPaths, visibilityPaths);
		Collections.reverse(allBuilds);
		allBuilds = filterByViewMode(allBuilds, tagPaths, viewMode);
		allBuilds = removeInvisibleBuilds(visibility, visibilityPaths, allBuilds);
		List<Build> pagedBuilds = pageBuilds(allBuilds, pageNumber, pageSize);
		addDataToBuilds(pagedBuilds, userPaths, tagPaths, includeBuildConfiguration, includeQAConfiguration, includeRvfURL);

		int totalPages = ListHelper.getTotalPages(allBuilds, pageSize);
		LOGGER.info("{} Builds being returned to client. {} pages of Builds available.", pagedBuilds.size(), totalPages);
		return new BuildPage<>(allBuilds.size(), totalPages, pageNumber, pageSize, pagedBuilds);
	}

	private List<Build> getAllBuildsFromS3(String productDirectoryPath, Product product, List<String> userPaths, List<String> tagPaths, List<String> visibilityPaths) {
		LOGGER.debug("Reading Builds in {}, {} in batches.", buildBucketName, productDirectoryPath);
		List<Build> builds = new ArrayList<>();
		final ListObjectsRequest listObjectsRequest = new ListObjectsRequest(buildBucketName, productDirectoryPath, null, null, 10000);
		ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
		boolean firstPass = true;
		while (firstPass || objectListing.isTruncated()) {
			if (!firstPass) {
				objectListing = s3Client.listNextBatchOfObjects(objectListing);
			}
			final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
			findBuilds(product, objectSummaries, builds, userPaths, tagPaths, visibilityPaths);
			firstPass = false;
		}

		LOGGER.debug("Found {} Builds in {}, {}.", builds.size(), buildBucketName, productDirectoryPath);
		return builds;
	}

	private List<Build> removeInvisibleBuilds(Boolean visibility, List<String> visibilityPaths, List<Build> builds) {
		LOGGER.debug("Removing invisible Builds.");
		if (visibility != null && !visibilityPaths.isEmpty() && !builds.isEmpty()) {
			List<String> invisibleBuildIds = getInvisibleBuilds(visibilityPaths);
			if (visibility) {
				return builds.stream()
						.filter(build -> !invisibleBuildIds.contains(build.getCreationTime()))
						.collect(Collectors.toList());
			} else {
				return builds.stream()
						.filter(build -> invisibleBuildIds.contains(build.getCreationTime()))
						.collect(Collectors.toList());
			}
		}

		LOGGER.debug("{} Builds remaining.", builds.size());
		return builds;
	}

	private List<Build> filterByViewMode(List<Build> allBuilds, List<String> tagPaths, BuildService.View viewMode) {
		switch (viewMode) {
			case PUBLISHED:
				allBuilds = allBuilds.stream().filter(build -> getTags(build, tagPaths) != null && getTags(build, tagPaths).contains(Tag.PUBLISHED)).collect(Collectors.toList());
				break;
			case UNPUBLISHED:
				allBuilds = allBuilds.stream().filter(build -> getTags(build, tagPaths) == null || !getTags(build, tagPaths).contains(Tag.PUBLISHED)).collect(Collectors.toList());
				break;
			case ALL_RELEASES:
				break;
			case DEFAULT:
				Build latestPublishedBuild = null;
				List<Build> publishedBuilds = new ArrayList<>();
				for (int i = 0; i < allBuilds.size(); i++) {
					Build build = allBuilds.get(i);
					if (getTags(build, tagPaths) != null && getTags(build, tagPaths).contains(Tag.PUBLISHED)) {
						if (latestPublishedBuild == null) {
							latestPublishedBuild = build;
						} else {
							publishedBuilds.add(build);
						}
					}
				}
				if (latestPublishedBuild != null) {
					allBuilds = allBuilds.subList(0, allBuilds.indexOf(latestPublishedBuild) + 1);
					allBuilds.addAll(publishedBuilds);
				}
				break;
		}
		return allBuilds;
	}

	private List<Build> pageBuilds(List<Build> builds, int pageNumber, int pageSize) {
		LOGGER.debug("Fetching pageNumber {} with pageSize {} from {} Builds.", pageNumber, pageSize, builds.size());
		return ListHelper.page(builds, pageNumber, pageSize);
	}

	private void addDataToBuilds(List<Build> builds, List<String> userPaths, List<String> tagPaths, Boolean includeBuildConfiguration, Boolean includeQAConfiguration, Boolean includeRvfURL) {
		LOGGER.info("Adding users, tags & build reports to Builds.");
		if (!builds.isEmpty()) {
			builds.forEach(build -> {
				build.setBuildUser(getBuildUser(build, userPaths));
				build.setTags(getTags(build, tagPaths));
				if (Boolean.TRUE.equals(includeRvfURL) &&
						(build.getStatus().equals(Build.Status.BUILT)
								|| build.getStatus().equals(Build.Status.RVF_QUEUED)
								|| build.getStatus().equals(Build.Status.RVF_RUNNING)
								|| build.getStatus().equals(Build.Status.RELEASE_COMPLETE)
								|| build.getStatus().equals(Build.Status.RELEASE_COMPLETE_WITH_WARNINGS))) {
					InputStream buildReportStream = getBuildReportFileStream(build);
					if (buildReportStream != null) {
						JSONParser jsonParser = new JSONParser();
						try {
							JSONObject jsonObject = (org.json.simple.JSONObject) jsonParser.parse(new InputStreamReader(buildReportStream, StandardCharsets.UTF_8));
							if (jsonObject.containsKey("rvf_response")) {
								build.setRvfURL(jsonObject.get("rvf_response").toString());
							}
						} catch (IOException e) {
							LOGGER.error("Error reading rvf_url from build_report file. Error: {}", e.getMessage());
						} catch (ParseException e) {
							LOGGER.error("Error parsing build_report file. Error: {}", e.getMessage());
						}
					}
				}
				if (Boolean.TRUE.equals(includeBuildConfiguration)) {
					try {
						this.loadBuildConfiguration(build);
					} catch (IOException e) {
						LOGGER.error("Error retrieving Build Configuration for build {}", build.getId());
					}
				}
				if (Boolean.TRUE.equals(includeQAConfiguration)) {
					try {
						this.loadQaTestConfig(build);
					} catch (IOException e) {
						LOGGER.error("Error retrieving QA Configuration for build {}", build.getId());
					}
				}
			});
		}
	}

	private void findBuilds(final Product product, final List<S3ObjectSummary> objectSummaries, final List<Build> builds, final List<String> userPaths, final List<String> tagPaths, final List<String>  visibilityPaths) {
		for (final S3ObjectSummary objectSummary : objectSummaries) {
			final String key = objectSummary.getKey();
			if (key.contains("/status:")) {
				final String[] keyParts = key.split("/");
				final String dateString = keyParts[keyParts.length - 2];
				final String status = keyParts[keyParts.length - 1].split(":")[1];
				final Build build = new Build(dateString, product.getBusinessKey(), status);
				build.setProduct(product);
				builds.add(build);
			} else if (key.contains("/tag:")) {
				tagPaths.add(key);
			} else if (key.contains("/user:")) {
				userPaths.add(key);
			} else if (key.contains("/visibility:")) {
				visibilityPaths.add(key);
			} else {
				// do nothing
			}
		}
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
		return null;
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
			final boolean visibility = Boolean.valueOf(keyParts[keyParts.length - 1].split(":")[1]);
			if (!visibility) {
				result.add(dateString);
			}
		}

		return result;
	}

	private AsyncPipedStreamBean getFileAsOutputStream(final String buildOutputFilePath) throws IOException {
		// Stream file to buildFileHelper as it's written to the OutputStream
		final PipedInputStream pipedInputStream = new PipedInputStream();
		final PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

		final Future<String> future = executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				srsFileHelper.putFile(pipedInputStream, buildOutputFilePath);
				LOGGER.debug("Build outputfile stream ended: {}", buildOutputFilePath);
				return buildOutputFilePath;
			}
		});

		return new AsyncPipedStreamBean(outputStream, future, buildOutputFilePath);
	}

	private PutObjectResult putFile(final String filePath, final String contents) {
		return s3Client.putObject(buildBucketName, filePath,
				new ByteArrayInputStream(contents.getBytes()), new ObjectMetadata());
	}

	@Required
	public void setBuildBucketName(final String buildBucketName) {
		this.buildBucketName = buildBucketName;
	}

	private File getLocalFile(final String transformedFilePath) throws FileNotFoundException {
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
			final String manifestFilePath = directoryPath + files.iterator().next();
			LOGGER.info("manifest file found at " + manifestFilePath);
			return manifestFilePath;
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
		final String reportFilePath = pathHelper.getBuildInputFilePrepareReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	@Override
	public boolean isBuildCancelRequested(final Build build) {
		if (Build.Status.CANCEL_REQUESTED.equals(build.getStatus())) return true;
		String cancelledRequestedPath = pathHelper.getStatusFilePath(build, Build.Status.CANCEL_REQUESTED);
		try {
			if (s3Client.getObject(buildBucketName, cancelledRequestedPath) != null) {
				build.setStatus(Build.Status.CANCEL_REQUESTED);
				LOGGER.warn("Build status is {}. Build will be cancelled when possible", build.getStatus().name());
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;

	}

	@Override
	public void deleteOutputFiles(Build build) {
		List<String> outputFiles = listOutputFilePaths(build);
		for (String outputFile : outputFiles) {
			if (srsFileHelper.exists(outputFile)) {
				srsFileHelper.deleteFile(outputFile);
			}
		}
	}

	@Override
	public InputStream getBuildInputGatherReportStream(Build build) {
		String reportFilePath = pathHelper.getBuildInputGatherReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}


	@Override
	public boolean isDerivativeProduct(Build build) {
		ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
		if (extensionConfig == null) {
			return false;
		}
		String releaseCenter = build.getProduct().getReleaseCenter().getBusinessKey();
		return INTERNATIONAL.equalsIgnoreCase(releaseCenter) && StringUtils.isNotBlank(extensionConfig.getDependencyRelease());

	}

	@Override
	public void updatePreConditionCheckReport(final Build build) throws IOException {
		File preConditionChecksReport = null;
		try {
			preConditionChecksReport = toJson(build.getPreConditionCheckReports());
			s3Client.putObject(buildBucketName, pathHelper.getBuildPreConditionCheckReportPath(build), new FileInputStream(preConditionChecksReport), new ObjectMetadata());
		} finally {
			if (preConditionChecksReport != null) {
				preConditionChecksReport.delete();
			}
		}
	}

	@Override
	public void updatePostConditionCheckReport(final Build build, final Object object) throws IOException {
		File postConditionChecksReport = null;
		try {
			postConditionChecksReport = toJson(object);
			s3Client.putObject(buildBucketName, pathHelper.getPostConditionCheckReportPath(build), new FileInputStream(postConditionChecksReport), new ObjectMetadata());
		} finally {
			if (postConditionChecksReport != null) {
				postConditionChecksReport.delete();
			}
		}
	}

	public InputStream getPreConditionCheckReportStream(final Build build) {
		final String reportFilePath = pathHelper.getBuildPreConditionCheckReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	public List<PreConditionCheckReport> getPreConditionCheckReport(final Build build) throws IOException {
		List<PreConditionCheckReport> reports = new ArrayList<>();
		final String reportFilePath = pathHelper.getBuildPreConditionCheckReportPath(build);
		final S3Object s3Object = s3Client.getObject(buildBucketName, reportFilePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
				reports = jsonParser.readValueAs(new TypeReference<List<PreConditionCheckReport>>(){});
			}
		}

		return reports;
	}

	@Override
	public InputStream getPostConditionCheckReportStream(final Build build) {
		final String reportFilePath = pathHelper.getPostConditionCheckReportPath(build);
		return srsFileHelper.getFileStream(reportFilePath);
	}

	public List<PostConditionCheckReport> getPostConditionCheckReport(final Build build) throws IOException {
		List<PostConditionCheckReport> reports = new ArrayList<>();
		final String reportFilePath = pathHelper.getPostConditionCheckReportPath(build);
		final S3Object s3Object = s3Client.getObject(buildBucketName, reportFilePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
				reports = jsonParser.readValueAs(new TypeReference<List<PostConditionCheckReport>>(){});
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
	public void updateVisibility(Build build, boolean visibility) {
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
	public void putManifestFile(Product product, String buildId, InputStream inputStream) {
		final String filePath = pathHelper.getBuildManifestDirectoryPath(product, buildId);
		srsFileHelper.putFile(inputStream, filePath + "manifest.xml");
	}

	@Override
	public void saveBuildComparisonReport(Product product, String compareId, BuildComparisonReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			s3Client.putObject(buildBucketName, pathHelper.getBuildComparisonReportPath(product, compareId), reportInputStream, new ObjectMetadata());
		} finally {
			if (reportFile != null) {
				reportFile.delete();
			}
		}
	}

	@Override
	public List<String> listBuildComparisonReportPaths(Product product) {
		final String reportPath = pathHelper.getBuildComparisonReportPath(product, null);
		return srsFileHelper.listFiles(reportPath);
	}

	@Override
	public BuildComparisonReport getBuildComparisonReport(Product product, String compareId) throws IOException {
		BuildComparisonReport report = null;
		String filePath = pathHelper.getBuildComparisonReportPath(product, compareId);
		final S3Object s3Object = s3Client.getObject(buildBucketName, filePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
				report = jsonParser.readValueAs(BuildComparisonReport.class);
			}
		}

		return report;
	}

	@Override
	public void saveFileComparisonReport(Product product, String compareId, FileDiffReport report) throws IOException {
		File reportFile = toJson(report);
		try (FileInputStream reportInputStream = new FileInputStream(reportFile)) {
			String reportFileName = report.getFileName().replace(".txt", ".diff.json");
			s3Client.putObject(buildBucketName, pathHelper.getFileComparisonReportPath(product, compareId, reportFileName), reportInputStream, new ObjectMetadata());
		} finally {
			if (reportFile != null) {
				reportFile.delete();

			}
		}
	}

	@Override
	public FileDiffReport getFileComparisonReport(Product product, String compareId, String fileName) throws IOException {
		FileDiffReport report = null;
		String reportFileName = fileName.replace(".txt", ".diff.json");
		String filePath = pathHelper.getFileComparisonReportPath(product, compareId, reportFileName);
		final S3Object s3Object = s3Client.getObject(buildBucketName, filePath);
		if (s3Object != null) {
			final S3ObjectInputStream objectContent = s3Object.getObjectContent();
			final String reportJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
			try (JsonParser jsonParser = objectMapper.getFactory().createParser(reportJson)) {
				report = jsonParser.readValueAs(FileDiffReport.class);
			}
		}

		return report;
	}
}
