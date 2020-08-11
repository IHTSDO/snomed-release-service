package org.ihtsdo.buildcloud.dao;

import com.amazonaws.services.s3.model.*;
import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.Rf2FileNameTransformation;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.telemetry.core.TelemetryStreamPathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
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
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.ihtsdo.buildcloud.entity.Build.Tag;

public class BuildDAOImpl implements BuildDAO {

    private static final String BLANK = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildDAOImpl.class);

    private static final String INTERNATIONAL = "international";

    private final ExecutorService executorService;

    private final FileHelper buildFileHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private final File tempDir;

    private final FileHelper publishedFileHelper;

    private final Rf2FileNameTransformation rf2FileNameTransformation;

    private S3Client s3Client;

    @Autowired
    private BuildS3PathHelper pathHelper;

    @Autowired
    private String buildBucketName;

    @Autowired
    private ProductInputFileDAO productInputFileDAO;

    @Autowired
    private Integer fileProcessingFailureMaxRetry;

    @Autowired
    public BuildDAOImpl(final String buildBucketName, final String publishedBucketName, final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
        executorService = Executors.newCachedThreadPool();
        buildFileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
        publishedFileHelper = new FileHelper(publishedBucketName, s3Client, s3ClientHelper);
        this.s3Client = s3Client;
        this.tempDir = Files.createTempDir();
        rf2FileNameTransformation = new Rf2FileNameTransformation();
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
        // Save status file
        updateStatus(build, Build.Status.BEFORE_TRIGGER);

        // Save trigger user
        if (StringUtils.isNotEmpty(build.getBuildUser())) {
            final String userFilePath = pathHelper.getBuildUserFilePath(build, build.getBuildUser());
            // Put new status before deleting old to avoid there being none.
            putFile(userFilePath, BLANK);
        }
        LOGGER.debug("Saved build {} with {} ", build.getId(), Build.Status.BEFORE_TRIGGER);
    }

    protected File toJson(final Object obj) throws IOException {
        final File temp = File.createTempFile("tempJson", ".tmp");
        final JsonFactory jsonFactory = objectMapper.getJsonFactory();
        try (JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(temp, JsonEncoding.UTF8)) {
            jsonGenerator.writeObject(obj);
        }
        return temp;
    }

    @Override
    public List<Build> findAllDesc(final Product product) {
        final String productDirectoryPath = pathHelper.getProductPath(product).toString();
        return findBuildsDesc(productDirectoryPath, product);
    }

    @Override
    public Build find(final Product product, final String buildId) {
        final String buildDirectoryPath = pathHelper.getBuildPath(product, buildId).toString();
        final List<Build> builds = findBuildsDesc(buildDirectoryPath, product);
        if (!builds.isEmpty()) {
            return builds.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void loadBuildConfiguration(final Build build) throws IOException {
        final String configFilePath = pathHelper.getBuildConfigFilePath(build);
        final S3Object s3Object = s3Client.getObject(buildBucketName, configFilePath);
        final S3ObjectInputStream objectContent = s3Object.getObjectContent();
        final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
        try (JsonParser jsonParser = objectMapper.getJsonFactory().createJsonParser(configurationJson)) {
            final BuildConfiguration buildConfiguration = jsonParser.readValueAs(BuildConfiguration.class);
            build.setConfiguration(buildConfiguration);
        }
    }


    @Override
    public void loadQaTestConfig(final Build build) throws IOException {
        final String configFilePath = pathHelper.getQATestConfigFilePath(build);
        final S3Object s3Object = s3Client.getObject(buildBucketName, configFilePath);
        final S3ObjectInputStream objectContent = s3Object.getObjectContent();
        final String configurationJson = FileCopyUtils.copyToString(new InputStreamReader(objectContent, RF2Constants.UTF_8));// Closes stream
        try (JsonParser jsonParser = objectMapper.getJsonFactory().createJsonParser(configurationJson)) {
            final QATestConfig qaTestConfig = jsonParser.readValueAs(QATestConfig.class);
            build.setQaTestConfig(qaTestConfig);
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
        }
    }

    @Override
    public void addTag(Build build, Tag tag) {
        build.setTag(tag);
        final String newTagFilePath = pathHelper.getTagFilePath(build, tag.name());
        putFile(newTagFilePath, BLANK);
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
        return buildFileHelper.getFileStream(outputFilePath);
    }

    @Override
    public List<String> listInputFileNames(final Build build) {
        final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build).toString();
        return buildFileHelper.listFiles(buildInputFilesPath);
    }

    @Override
    public InputStream getInputFileStream(final Build build, final String inputFile) {
        final String path = pathHelper.getBuildInputFilePath(build, inputFile);
        return buildFileHelper.getFileStream(path);
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
        buildFileHelper.copyFile(buildInputFilePath, buildOutputFilePath);
    }

    @Override
    public void copyAll(final Product product, final Build build) throws IOException {
        // Copy input files
        final String productInputFilesPath = pathHelper.getProductInputFilesPath(product);
        final String buildInputFilesPath = pathHelper.getBuildInputFilesPath(build).toString();
        final List<String> filePaths = productInputFileDAO.listRelativeInputFilePaths(product);
        for (final String filePath : filePaths) {
            try {
                buildFileHelper.copyFile(productInputFilesPath + filePath, buildInputFilesPath + filePath);
            } catch (AmazonS3Exception e) {
                if (fileProcessingFailureMaxRetry != null) {
                    int attempt = 1;
                    boolean copiedSuccessfully = false;
                    do {
                        LOGGER.warn("Failed to copy file {} from S3 product input-files bucket {} to build input-file bucket {} on attempt {}. Waiting {} seconds before retrying.", filePath, productInputFilesPath, buildInputFilesPath, attempt, 10);
                        try {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException ex) {
                                LOGGER.warn("Retry delay interrupted.", e);
                            }
                            buildFileHelper.copyFile(productInputFilesPath + filePath, buildInputFilesPath + filePath);
                            List<String> buildInputFileNames = listInputFileNames(build);
                            copiedSuccessfully = buildInputFileNames.contains(filePath);
                        } catch (AmazonS3Exception ex) {
                            // do nothing
                        } finally {
                            attempt++;
                        }
                    } while (!copiedSuccessfully && attempt < fileProcessingFailureMaxRetry + 1);
                }
            }
        }

        // Copy manifest file
        final String manifestPath = productInputFileDAO.getManifestPath(product);
        if (manifestPath != null) { // Let the packages with manifests product
            final String buildManifestDirectoryPath = pathHelper.getBuildManifestDirectoryPath(build);
            final String manifestFileName = Paths.get(manifestPath).getFileName().toString();
            buildFileHelper.copyFile(manifestPath, buildManifestDirectoryPath + manifestFileName);
        }
        //copy input-prepare-report.json if exists
        try (InputStream inputReportStream = productInputFileDAO.getInputPrepareReport(product)) {
            if (inputReportStream != null) {
                buildFileHelper.putFile(inputReportStream, pathHelper.getBuildInputFilePrepareReportPath(build));
            }
        }

        //copy sources-gather-report.json if exists
        InputStream sourcesGatherStream = productInputFileDAO.getInputGatherReport(product);
        if (sourcesGatherStream != null) {
            buildFileHelper.putFile(sourcesGatherStream, pathHelper.getBuildInputGatherReportPath(build));
        }
    }

    @Override
    public InputStream getOutputFileInputStream(final Build build, final String name) {
        final String path = pathHelper.getBuildOutputFilePath(build, name);
        return buildFileHelper.getFileStream(path);
    }

    @Override
    public String putOutputFile(final Build build, final File file, final boolean calcMD5) throws IOException {
        final String filename = file.getName();
        final String outputFilePath = pathHelper.getBuildOutputFilePath(build, filename);
        try {
            return buildFileHelper.putFile(file, outputFilePath, calcMD5);
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
            return buildFileHelper.putFile(file, inputFilePath, calcMD5);
        } catch (NoSuchAlgorithmException | DecoderException e) {
            throw new IOException("Problem creating checksum while uploading " + filename, e);
        }
    }

    @Override
    public void putTransformedFile(final Build build, final File file) throws IOException {
        final String name = file.getName();
        final String outputPath = pathHelper.getBuildTransformedFilesPath(build).append(name).toString();
        try {
            buildFileHelper.putFile(file, outputPath);
        } catch (NoSuchAlgorithmException | DecoderException e) {
            throw new IOException("Problem creating checksum while uploading transformed file " + name, e);
        }
    }

    @Override
    public InputStream getManifestStream(final Build build) {
        String manifestFilePath = getManifestFilePath(build);
        if (manifestFilePath != null) {
            LOGGER.info("Opening manifest file found at " + manifestFilePath);
            return buildFileHelper.getFileStream(manifestFilePath);
        } else {
            LOGGER.error("Failed to find manifest file for " + build.getId());
            return null;
        }
    }

    @Override
    public List<String> listTransformedFilePaths(final Build build) {
        final String transformedFilesPath = pathHelper.getBuildTransformedFilesPath(build).toString();
        return buildFileHelper.listFiles(transformedFilesPath);
    }

    @Override
    public List<String> listOutputFilePaths(final Build build) {
        final String outputFilesPath = pathHelper.getOutputFilesPath(build);
        return buildFileHelper.listFiles(outputFilesPath);
    }

    @Override
    public List<String> listLogFilePaths(final Build build) {
        final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
        return buildFileHelper.listFiles(logFilesPath);
    }

    @Override
    public List<String> listBuildLogFilePaths(final Build build) {
        final String logFilesPath = pathHelper.getBuildLogFilesPath(build).toString();
        return buildFileHelper.listFiles(logFilesPath);
    }

    @Override
    public InputStream getLogFileStream(final Build build, final String logFileName) {
        final String logFilePath = pathHelper.getBuildLogFilePath(build, logFileName);
        return buildFileHelper.getFileStream(logFilePath);
    }

    @Override
    public InputStream getBuildLogFileStream(final Build build, final String logFileName) {
        final String logFilePath = pathHelper.getBuildLogFilePath(build, logFileName);
        return buildFileHelper.getFileStream(logFilePath);
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
        return buildFileHelper.getFileStream(transformedFilePath);
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

        final List<String> filePaths = publishedFileHelper.listFiles(publishedExtractedZipPath);
        for (final String filePath : filePaths) {
            String filename = FileUtils.getFilenameFromPath(filePath);
            // use contains rather that startsWith so that we can have candidate release (with x prefix in the filename)
            // as previous published release.
            if (!Normalizer.isNormalized(filename, Normalizer.Form.NFC)) {
                filename = Normalizer.normalize(filename, Normalizer.Form.NFC);
            }
            if (filename.contains(targetFileNameStripped)) {
                return publishedFileHelper.getFileStream(publishedExtractedZipPath + filePath);
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
            buildFileHelper.putFile(is, buildReportJSON.length(), reportPath);
        } catch (final IOException e) {
            LOGGER.error("Unable to persist build report", e);
        }
    }

    @Override
    public void renameTransformedFile(final Build build, final String sourceFileName, final String targetFileName, boolean deleteOriginal) {
        final String soureFilePath = pathHelper.getTransformedFilePath(build, sourceFileName);
        final String targetFilePath = pathHelper.getTransformedFilePath(build, targetFileName);
        buildFileHelper.copyFile(soureFilePath, targetFilePath);
        if (deleteOriginal) {
            buildFileHelper.deleteFile(soureFilePath);
        }
    }

    private List<Build> findBuildsDesc(final String productDirectoryPath, final Product product) {
        final List<Build> builds = new ArrayList<>();
        LOGGER.info("List s3 objects {}, {}", buildBucketName, productDirectoryPath);

        // Not easy to make this efficient because our timestamp immediately under the product name means that we can only prefix
        // with the product name. The S3 API doesn't allow us to pattern match just the status files.
        // I think an "index" directory might be the solution

        // I think adding a pipe to the end of the status filename and using that as the delimiter would be
        // the simplest way to give performance - KK

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest(buildBucketName, productDirectoryPath, null, null, 10000);
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

        boolean firstPass = true;
        while (firstPass || objectListing.isTruncated()) {
            if (!firstPass) {
                objectListing = s3Client.listNextBatchOfObjects(objectListing);
            }
            final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            findBuilds(product, objectSummaries, builds);
            firstPass = false;
        }

        LOGGER.debug("Found {} Builds", builds.size());
        Collections.reverse(builds);
        return builds;
    }

    private void findBuilds(final Product product, final List<S3ObjectSummary> objectSummaries, final List<Build> builds) {
        for (final S3ObjectSummary objectSummary : objectSummaries) {
            final String key = objectSummary.getKey();
            if (key.contains("/status:")) {
                LOGGER.debug("Found status key {}", key);
                final String[] keyParts = key.split("/");
                final String dateString = keyParts[2];
                final String status = keyParts[3].split(":")[1];
                final Build build = new Build(dateString, product.getBusinessKey(), status);
                build.setBuildUser(getBuildUser(dateString, objectSummaries));
                build.setTag(getTag(build, objectSummaries));
                build.setProductName(product.getName());
                builds.add(build);
            }
        }
    }

    private Tag getTag(Build build, List<S3ObjectSummary> objectSummaries) {
        for (final S3ObjectSummary objectSummary : objectSummaries) {
            final String key = objectSummary.getKey();
            if (key.contains("/tag:")) {
                final String[] keyParts = key.split("/");
                final String dateString = keyParts[2];
                final String tagString = keyParts[3].split(":")[1];
                if (build.getCreationTime().equals(dateString)) {
                    return Build.Tag.valueOf(tagString);
                }
            }
        }
        return null;
    }

    private String getBuildUser(final String creationTime, final List<S3ObjectSummary> objectSummaries) {
        for (final S3ObjectSummary objectSummary : objectSummaries) {
            final String key = objectSummary.getKey();
            if (key.contains("/user:")) {
                final String[] keyParts = key.split("/");
                final String dateString = keyParts[2];
                final String user = keyParts[3].split(":")[1];
                if (creationTime.equals(dateString)) {
                    return user;
                }
            }
        }

        return User.ANONYMOUS_USER;
    }

    private AsyncPipedStreamBean getFileAsOutputStream(final String buildOutputFilePath) throws IOException {
        // Stream file to buildFileHelper as it's written to the OutputStream
        final PipedInputStream pipedInputStream = new PipedInputStream();
        final PipedOutputStream outputStream = new PipedOutputStream(pipedInputStream);

        final Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                buildFileHelper.putFile(pipedInputStream, buildOutputFilePath);
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

    // Just for testing
    protected void setS3Client(final S3Client s3Client) {
        this.s3Client = s3Client;
        this.buildFileHelper.setS3Client(s3Client);
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
        final List<String> files = buildFileHelper.listFiles(directoryPath);
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
        return buildFileHelper.getFileStream(reportFilePath);
    }

    @Override
    public InputStream getBuildInputFilesPrepareReportStream(Build build) {
        final String reportFilePath = pathHelper.getBuildInputFilePrepareReportPath(build);
        return buildFileHelper.getFileStream(reportFilePath);
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
            if (buildFileHelper.exists(outputFile)) {
                buildFileHelper.deleteFile(outputFile);
            }
        }
    }

    @Override
    public InputStream getBuildInputGatherReportStream(Build build) {
        String reportFilePath = pathHelper.getBuildInputGatherReportPath(build);
        return buildFileHelper.getFileStream(reportFilePath);
    }


    @Override
    public boolean isDerivativeProduct(Build build) {
        ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
        if (extensionConfig == null) {
            return false;
        }
        String releaseCenter = build.getProduct().getReleaseCenter().getBusinessKey();
        if (INTERNATIONAL.equalsIgnoreCase(releaseCenter) && StringUtils.isNotBlank(extensionConfig.getDependencyRelease())) {
            return true;
        }
        return false;

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
        return buildFileHelper.getFileStream(reportFilePath);
    }

    @Override
    public InputStream getPostConditionCheckReportStream(final Build build) {
        final String reportFilePath = pathHelper.getPostConditionCheckReportPath(build);
        return buildFileHelper.getFileStream(reportFilePath);
    }

    @Override
    public List<String> listClassificationResultOutputFileNames(Build build) {
        final String buildInputFilesPath = pathHelper.getClassificationResultOutputFilePath(build).toString();
        return buildFileHelper.listFiles(buildInputFilesPath);
    }

    @Override
    public String putClassificationResultOutputFile(final Build build, final File file) throws IOException {
        final String filename = file.getName();
        final String outputFilePath = pathHelper.getClassificationResultOutputPath(build, filename);
        try {
            return buildFileHelper.putFile(file, outputFilePath, false);
        } catch (NoSuchAlgorithmException | DecoderException e) {
            throw new IOException("Problem creating checksum while uploading " + filename, e);
        }
    }

    @Override
    public InputStream getClassificationResultOutputFileStream(Build build, String relativeFilePath) {
        final String path = pathHelper.getClassificationResultOutputPath(build, relativeFilePath);
        return buildFileHelper.getFileStream(path);
    }
}
