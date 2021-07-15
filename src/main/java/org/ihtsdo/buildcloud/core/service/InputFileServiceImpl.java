package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.BuildRequestPojo;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.InputFileDAO;
import org.ihtsdo.buildcloud.core.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.InputSourceFileProcessor;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType.ERROR;
import static org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType.WARNING;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	public static final Logger LOGGER = LoggerFactory.getLogger(InputFileServiceImpl.class);

	private final FileHelper fileHelper;

	private final FileHelper externallyMaintainedFileHelper;

	private static final String SRC_TERM_SERVER = "terminology-server";

	private static final String SRC_EXT_MAINTAINED = "externally-maintained";

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private BuildS3PathHelper s3PathHelper;

	@Autowired
	private TermServerService termServerService;

	@Value("${srs.file-processing.failureMaxRetry}")
	private Integer fileProcessingFailureMaxRetry;

	private final String buildBucketName;

	@Autowired
	public InputFileServiceImpl(@Value("${srs.build.bucketName}") final String buildBucketName,
							@Value("${srs.build.externally-maintained-bucketName}") final String externallyMaintainedBucketName,
							final S3Client s3Client,
							final S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
		this.buildBucketName = buildBucketName;
		externallyMaintainedFileHelper = new FileHelper(externallyMaintainedBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void putManifestFile(String centerKey, final String productKey, final InputStream inputStream, final String originalFilename, final long fileSize) throws ResourceNotFoundException {
		Product product = constructProduct(centerKey, productKey);
		inputFileDAO.putManifestFile(product, inputStream, originalFilename, fileSize);
	}

	@Override
	public String getManifestFileName(String centerKey, final String productKey) throws ResourceNotFoundException {
		return inputFileDAO.getManifestPath(constructProduct(centerKey, productKey));
	}

	@Override
	public InputStream getManifestStream(String centerKey, final String productKey) throws ResourceNotFoundException {
		return inputFileDAO.getManifestStream(constructProduct(centerKey, productKey));
	}

	@Override
	public void putInputFile(final String centerKey, final Product product, final String buildId, final InputStream inputStream, final String filename, final long fileSize) throws IOException {
		String buildInputFilesPath = s3PathHelper.getBuildInputFilesPath(product, buildId).toString();
		putFile(filename, inputStream, buildInputFilesPath, fileSize);

	}

	@Override
	public void putSourceFile(String sourceName, String centerKey, String productKey, String buildId, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException {
		Product product = constructProduct(centerKey, productKey);
		if(StringUtils.isBlank(sourceName)) throw new IllegalArgumentException("sourceName cannot be empty");

		String sourceFilesPath = s3PathHelper.getBuildSourceSubDirectoryPath(product, buildId, sourceName).toString();
		putSourceFile(filename, inputStream, sourceFilesPath, fileSize);

	}

	@Override
	public List<String> listSourceFilePaths(String centerKey, String productKey, String buildId) throws ResourceNotFoundException {
		Product product = constructProduct(centerKey, productKey);
		return inputFileDAO.listRelativeSourceFilePaths(product, buildId);
	}

	@Override
	public List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, Set<String> subDirectories, String buildId) throws ResourceNotFoundException {
		Product product = constructProduct(centerKey, productKey);
		return inputFileDAO.listRelativeSourceFilePaths(product, buildId, subDirectories);
	}

	@Override
	public SourceFileProcessingReport prepareInputFiles(String centerKey, String productKey, String buildId, boolean copyFilesInManifest) throws BusinessServiceException {
		Product product = constructProduct(centerKey, productKey);
		InputSourceFileProcessor fileProcessor = new InputSourceFileProcessor(fileHelper, s3PathHelper, product, copyFilesInManifest);
		// check manifest file is present and valid
		SourceFileProcessingReport report = fileProcessor.getFileProcessingReport();
		checkAndValidateManifestFile(report, product, buildId);
		if (!report.getDetails().containsKey(ERROR)) {
			try (InputStream manifestInputStream = inputFileDAO.getManifestStream(product, buildId)) {
				List<String> sourceFiles = listSourceFilePaths(centerKey, productKey, buildId);
				if (sourceFiles != null && !sourceFiles.isEmpty()) {
					fileProcessor.processFiles(manifestInputStream, sourceFiles, buildId, fileProcessingFailureMaxRetry);
				} else {
					report.add(ERROR, "Failed to find files from source directory");
				}
				for (String source : fileProcessor.getSkippedSourceFiles().keySet()) {
					for (String skippedFile : fileProcessor.getSkippedSourceFiles().get(source)) {
						report.add(WARNING, FilenameUtils.getName(skippedFile), null, source, "skipped processing");
					}
				}
			} catch (Exception e) {
				String errorMsg = String.format("Failed to prepare input files successfully for product %s", productKey);
				LOGGER.error(errorMsg, e);
				report.add(ReportType.ERROR, errorMsg);
			}
		}

		try {
			inputFileDAO.persistInputPrepareReport(product, buildId, report);
		} catch (IOException e) {
			throw new BusinessServiceException(e);
		}
		return report;
	}

	private void checkAndValidateManifestFile(SourceFileProcessingReport report, Product product, String buildId) throws BusinessServiceException {
		try (InputStream manifestStream = inputFileDAO.getManifestStream(product, buildId)) {
			if (manifestStream == null) {
				report.add(ERROR, String.format("No manifest.xml found for product %s and build %s", product.getBusinessKey(), buildId));
			} else {
				//validate manifest.xml
				String validationMsg = ManifestValidator.validate(manifestStream);
				if (validationMsg != null) {
					String errorMsg = "manifest.xml doesn't conform to the schema definition. " + validationMsg;
					report.add(ReportType.ERROR, errorMsg);
				}
			}
		} catch (IOException e) {
			throw new BusinessServiceException(String.format("Failed to load manifest.xml for %s %s", product.getBusinessKey(), buildId), e);
		}
	}

	private Product constructProduct(String centerKey, final String productKey) {
		LOGGER.debug("ReleaseCenter=" + centerKey + " productKey =" + productKey);
		ReleaseCenter releaseCenter = new ReleaseCenter();
		releaseCenter.setShortName(centerKey);
		Product product = new Product();
		product.setReleaseCenter(releaseCenter);
		product.setBusinessKey(productKey);
		return product;
	}

	private void putFile(String filename, InputStream inputStream, String filePath, long fileSize) throws IOException {
		if (filename.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			Path tempFile = Files.createTempFile(getClass().getCanonicalName(), ".zip");
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					String fileDestinationPath = filePath + FileUtils.getFilenameFromPath(entry.getName());
					Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
					try (FileInputStream tempFileInputStream = new FileInputStream(tempFile.toFile())) {
						fileHelper.putFile(tempFileInputStream, tempFile.toFile().length(), fileDestinationPath);
					}
				}
			} finally {
				if (!tempFile.toFile().delete()) {
					LOGGER.warn("Failed to delete temp file {}", tempFile.toFile().getAbsolutePath());
				}
			}
		} else {
			String fileDestinationPath = filePath + filename;
			fileHelper.putFile(inputStream, fileSize, fileDestinationPath);
		}
	}

	private void putSourceFile(String filename, InputStream inputStream, String filePath, long fileSize) throws IOException {
		if (filename.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			Path tempFile = Files.createTempFile(getClass().getCanonicalName(), ".zip");
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					if(!entry.isDirectory()) {
						String fileDestinationPath = filePath + FileUtils.getFilenameFromPath(entry.getName());
						Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
						try (FileInputStream tempFileInputStream = new FileInputStream(tempFile.toFile())) {
							fileHelper.putFile(tempFileInputStream, tempFile.toFile().length(), fileDestinationPath);
						}
					}
				}
			} finally {
				if (!tempFile.toFile().delete()) {
					LOGGER.warn("Failed to delete temp file {}", tempFile.toFile().getAbsolutePath());
				}
			}
		} else {
			String fileDestinationPath = filePath + filename;
			fileHelper.putFile(inputStream, fileSize, fileDestinationPath);
		}
	}

	@Override
	public InputGatherReport gatherSourceFiles(String centerKey, String productKey, String buildId, BuildRequestPojo requestConfig, SecurityContext securityContext) {
		InputGatherReport inputGatherReport = new InputGatherReport();
		try {
			Product product = constructProduct(centerKey, productKey);
			inputFileDAO.persistSourcesGatherReport(product, buildId, inputGatherReport);
			if (!requestConfig.isSkipGatheringSourceFiles()) {
				if (requestConfig.isLoadTermServerData()) {
					gatherSourceFilesFromTermServer(centerKey, productKey, buildId, requestConfig, inputGatherReport, securityContext);
				}
				if (requestConfig.isLoadExternalRefsetData()) {
					gatherSourceFilesFromExternallyMaintainedBucket(centerKey, productKey, buildId, requestConfig.getEffectiveDate(), inputGatherReport);
				}
			}
			inputGatherReport.setStatus(InputGatherReport.Status.COMPLETED);
			inputFileDAO.persistSourcesGatherReport(product, buildId, inputGatherReport);
		} catch (Exception ex) {
			LOGGER.error("Failed to gather source files!", ex);
			inputGatherReport.setStatus(InputGatherReport.Status.ERROR);
		}
		return inputGatherReport;
	}

	private void gatherSourceFilesFromTermServer(String centerKey, String productKey, String buildId, BuildRequestPojo requestConfig
			, InputGatherReport inputGatherReport, SecurityContext securityContext) throws BusinessServiceException, IOException {
		File fileExported = null;
		try {
			SecurityContextHolder.setContext(securityContext);
			fileExported = termServerService.export(requestConfig.getBranchPath(), requestConfig.getEffectiveDate(),
					requestConfig.getExcludedModuleIds(), requestConfig.getExportCategory());
			//Test whether the exported file is really a zip file
			try (ZipFile zipFile = new ZipFile(fileExported);
			FileInputStream fileInputStream = new FileInputStream(fileExported);) {
				putSourceFile(SRC_TERM_SERVER, centerKey, productKey, buildId, fileInputStream, fileExported.getName(), fileExported.length());
				inputGatherReport.addDetails(InputGatherReport.Status.COMPLETED, SRC_TERM_SERVER,
						"Successfully export file " + fileExported.getName() + " from term server and upload to source \"terminology-server\"");
				LOGGER.info("Successfully export file {} from term server and upload to source \"terminology-server\"", fileExported.getName());
			}
		} catch (Exception ex) {
			inputGatherReport.addDetails(InputGatherReport.Status.ERROR, SRC_TERM_SERVER, ex.getMessage());
			throw ex;
		} finally {
			SecurityContextHolder.clearContext();
			org.apache.commons.io.FileUtils.deleteQuietly(fileExported);

		}
	}


	public void gatherSourceFilesFromExternallyMaintainedBucket(String centerKey, String productKey, String buildId, String effectiveDate, InputGatherReport inputGatherReport) throws IOException {
		String dirPath = centerKey + "/" + effectiveDate + "/";
		List<String> externalFiles = externallyMaintainedFileHelper.listFiles(dirPath);
		LOGGER.info("Found {} files at {} in external maintained bucket", externalFiles.size(), dirPath);
		for (String externalFile : externalFiles) {
			// Skip if current object is a directory
			if (StringUtils.isBlank(externalFile) || externalFile.endsWith("/")) {
				continue;
			}
			try {
				Product product = constructProduct(centerKey, productKey);
				String sourceFilesPath = s3PathHelper.getBuildSourceSubDirectoryPath(product, buildId, SRC_EXT_MAINTAINED).toString();
				externallyMaintainedFileHelper.copyFile(dirPath + externalFile, buildBucketName, sourceFilesPath + FilenameUtils.getName(externalFile));
				LOGGER.info("Successfully export file " + externalFile + " from Externally Maintained bucket and uploaded to source \"externally-maintained\"");
			} catch (Exception ex) {
				LOGGER.error("Failed to pull external file from S3: {}/{}/{}", centerKey, effectiveDate, externalFile, ex);
				inputGatherReport.addDetails(InputGatherReport.Status.ERROR, SRC_EXT_MAINTAINED, ex.getMessage());
				throw ex;
			}
		}
	}

	@Override
	public InputStream getSourceFileStream(String releaseCenterKey, String productKey, String source, String sourceFileName) {
		Product product = constructProduct(releaseCenterKey, productKey);
		return fileHelper.getFileStream(s3PathHelper.getProductSourcesPath(product) + source + BuildS3PathHelper.SEPARATOR + sourceFileName);
	}

}
