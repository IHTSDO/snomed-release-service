package org.ihtsdo.buildcloud.core.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.InputFileDAO;
import org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.InputSourceFileProcessor;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.client.terminologyserver.SnowstormRestClient;
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

import java.io.*;
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

	private static final String SRC_TERM_SERVER = "terminology-server";

	private static final String SRC_EXT_MAINTAINED = "externally-maintained";

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private S3PathHelper s3PathHelper;

	@Autowired
	private TermServerService termServerService;

	@Value("${srs.file-processing.failureMaxRetry}")
	private Integer fileProcessingFailureMaxRetry;

	@Autowired
	public InputFileServiceImpl(@Value("${srs.storage.bucketName}") final String storageBucketName,
							final S3Client s3Client) {
		fileHelper = new FileHelper(storageBucketName, s3Client);
	}

	@Override
	public void putManifestFile(final String centerKey, final String productKey, final InputStream inputStream, final String originalFilename, final long fileSize) throws ResourceNotFoundException {
		inputFileDAO.putManifestFile(centerKey, productKey, inputStream, originalFilename, fileSize);
	}

	@Override
	public String getManifestFileName(final String centerKey, final String productKey) throws ResourceNotFoundException {
		return inputFileDAO.getManifestPath(centerKey, productKey);
	}

	@Override
	public InputStream getManifestStream(final String centerKey, final String productKey) throws ResourceNotFoundException {
		return inputFileDAO.getManifestStream(centerKey, productKey);
	}

	@Override
	public void putInputFile(final String centerKey, final String productKey, final String buildId, final InputStream inputStream, final String filename, final long fileSize) throws IOException {
		String buildInputFilesPath = s3PathHelper.getBuildInputFilesPath(centerKey, productKey, buildId).toString();
		putFile(filename, inputStream, buildInputFilesPath, fileSize);
	}

	@Override
	public void putSourceFile(String sourceName, String centerKey, String productKey, String buildId, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException {
		if (StringUtils.isBlank(sourceName)) throw new IllegalArgumentException("sourceName cannot be empty");

		String sourceFilesPath = s3PathHelper.getBuildSourceSubDirectoryPath(centerKey, productKey, buildId, sourceName).toString();
		putSourceFile(filename, inputStream, sourceFilesPath, fileSize);
	}

	@Override
	public List<String> listSourceFilePaths(String centerKey, String productKey, String buildId) throws ResourceNotFoundException {
		return inputFileDAO.listRelativeSourceFilePaths(centerKey, productKey, buildId);
	}

	@Override
	public List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, String buildId, Set<String> subDirectories) throws ResourceNotFoundException {
		return inputFileDAO.listRelativeSourceFilePaths(centerKey, productKey, buildId, subDirectories);
	}

	@Override
	public SourceFileProcessingReport prepareInputFiles(Build build, boolean copyFilesInManifest) throws BusinessServiceException {
		InputSourceFileProcessor fileProcessor = new InputSourceFileProcessor(fileHelper, s3PathHelper, build.getReleaseCenterKey(), build.getProductKey(), copyFilesInManifest);
		// check manifest file is present and valid
		SourceFileProcessingReport report = fileProcessor.getFileProcessingReport();
		checkAndValidateManifestFile(report, build);
		if (!report.getDetails().containsKey(ERROR)) {
			try (InputStream manifestInputStream = inputFileDAO.getManifestStream(build.getReleaseCenterKey(), build.getProductKey(), build.getId())) {
				List<String> sourceFiles = listSourceFilePaths(build.getReleaseCenterKey(), build.getProductKey(), build.getId());
				if (sourceFiles != null && !sourceFiles.isEmpty()) {
					fileProcessor.processFiles(manifestInputStream, sourceFiles, build.getId(), fileProcessingFailureMaxRetry);
				} else {
					if (build.getConfiguration().isLoadExternalRefsetData() || build.getConfiguration().isLoadTermServerData()) {
						// add to error report as source files used but failed to find any
						report.add(ERROR, "Failed to find files from source directory");
					}
				}
				for (String source : fileProcessor.getSkippedSourceFiles().keySet()) {
					for (String skippedFile : fileProcessor.getSkippedSourceFiles().get(source)) {
						report.add(WARNING, FilenameUtils.getName(skippedFile), null, source, "skipped processing");
					}
				}
			} catch (Exception e) {
				String errorMsg = String.format("Failed to prepare input files successfully for product %s", build.getProductKey());
				LOGGER.error(errorMsg, e);
				report.add(ReportType.ERROR, errorMsg);
			}
		}

		try {
			inputFileDAO.persistInputPrepareReport(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), report);
		} catch (IOException e) {
			throw new BusinessServiceException("Failed to persist inputPrepareReport to S3", e);
		}
		return report;
	}

	private void checkAndValidateManifestFile(SourceFileProcessingReport report, Build build) throws BusinessServiceException {
		try (InputStream manifestStream = inputFileDAO.getManifestStream(build.getReleaseCenterKey(), build.getProductKey(), build.getId())) {
			if (manifestStream == null) {
				report.add(ERROR, String.format("No manifest.xml found for product %s and build %s", build.getProductKey(), build.getId()));
			} else {
				//validate manifest.xml
				String validationMsg = ManifestValidator.validate(manifestStream);
				if (validationMsg != null) {
					String errorMsg = "manifest.xml doesn't conform to the schema definition. " + validationMsg;
					report.add(ReportType.ERROR, errorMsg);
				}
			}
		} catch (IOException e) {
			throw new BusinessServiceException(String.format("Failed to load manifest.xml for %s %s", build.getProductKey(), build.getId()), e);
		}
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
	public InputGatherReport gatherSourceFiles(Build build, SecurityContext securityContext) throws BusinessServiceException {
		InputGatherReport inputGatherReport = new InputGatherReport();
		try {
			BuildConfiguration buildConfiguration = build.getConfiguration();
			inputFileDAO.persistSourcesGatherReport(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), inputGatherReport);
			if (buildConfiguration.isLoadTermServerData()) {
				gatherSourceFilesFromTermServer(build, inputGatherReport, securityContext);
			}
			if (buildConfiguration.isLoadExternalRefsetData()) {
				gatherSourceFilesFromExternallyMaintained(build, inputGatherReport);
			}
			inputGatherReport.setStatus(InputGatherReport.Status.COMPLETED);
		} catch (Exception ex) {
			LOGGER.error("Failed to gather source files!", ex);
			inputGatherReport.setStatus(InputGatherReport.Status.ERROR);
		} finally {
			try {
				inputFileDAO.persistSourcesGatherReport(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), inputGatherReport);
			} catch (IOException e) {
				throw new BusinessServiceException("Failed to persist inputGatherReport to S3");
			}
		}
		return inputGatherReport;
	}

	private void gatherSourceFilesFromTermServer(Build build, InputGatherReport inputGatherReport, SecurityContext securityContext) throws BusinessServiceException {
		File fileExported = null;
		try {
			SecurityContextHolder.setContext(securityContext);
			BuildConfiguration buildConfiguration = build.getConfiguration();
			final long branchHeadTimestamp = termServerService.getBranch(buildConfiguration.getBranchPath()).getHeadTimestamp();
			fileExported = termServerService.export(buildConfiguration.getBranchPath(), buildConfiguration.getEffectiveTimeSnomedFormat(),
					buildConfiguration.getExtensionConfig() != null ? buildConfiguration.getExtensionConfig().getModuleIdsSet() : null, SnowstormRestClient.ExportCategory.valueOf(buildConfiguration.getExportType()));
			build.getQaTestConfig().setContentHeadTimestamp(branchHeadTimestamp);
			buildDAO.updateQATestConfig(build);
			//Test whether the exported file is really a zip file
			try (ZipFile zipFile = new ZipFile(fileExported);
			FileInputStream fileInputStream = new FileInputStream(fileExported);) {
				putSourceFile(SRC_TERM_SERVER, build.getReleaseCenterKey(), build.getProductKey(), build.getId(), fileInputStream, fileExported.getName(), fileExported.length());
				inputGatherReport.addDetails(InputGatherReport.Status.COMPLETED, SRC_TERM_SERVER,
						"Successfully export file " + fileExported.getName() + " from term server and upload to source \"terminology-server\"");
				LOGGER.info("Successfully export file {} from term server and upload to source \"terminology-server\"", fileExported.getName());
			}
		} catch (Exception ex) {
			inputGatherReport.addDetails(InputGatherReport.Status.ERROR, SRC_TERM_SERVER, ex.getMessage());
			throw new BusinessServiceException("Failed to export from term server for branch " + build.getConfiguration().getBranchPath() + ". Error: " + ex.getMessage());
		} finally {
			SecurityContextHolder.clearContext();
			org.apache.commons.io.FileUtils.deleteQuietly(fileExported);
		}
	}

	private void gatherSourceFilesFromExternallyMaintained(Build build, InputGatherReport inputGatherReport) throws BusinessServiceException {
		BuildConfiguration configuration = build.getConfiguration();
		String externallyMaintainedPath = s3PathHelper.getExternallyMaintainedDirectoryPath(build.getReleaseCenterKey(), configuration.getEffectiveTimeSnomedFormat());
		List<String> externalFiles = fileHelper.listFiles(externallyMaintainedPath);
		LOGGER.info("Found {} files at {} in storage bucket", externalFiles.size(), externallyMaintainedPath);
		for (String externalFile : externalFiles) {
			// Skip if current object is a directory
			if (StringUtils.isBlank(externalFile) || externalFile.endsWith(S3PathHelper.SEPARATOR)) {
				continue;
			}
			try {
				String sourceFilesPath = s3PathHelper.getBuildSourceSubDirectoryPath(build, SRC_EXT_MAINTAINED).toString();
				fileHelper.copyFile(externallyMaintainedPath + externalFile, sourceFilesPath + FilenameUtils.getName(externalFile));
				LOGGER.info("Successfully exported file " + externalFile + " from " + externallyMaintainedPath + " and uploaded to source \"externally-maintained\"");
			} catch (Exception ex) {
				LOGGER.error("Failed to pull external file from S3: {}/{}/{}", build.getReleaseCenterKey(), configuration.getEffectiveTimeFormatted(), externalFile, ex);
				inputGatherReport.addDetails(InputGatherReport.Status.ERROR, SRC_EXT_MAINTAINED, ex.getMessage());
				throw new BusinessServiceException("Failed to pull external file from S3. Error: " + ex.getMessage());
			}
		}
	}

	@Override
	public InputStream getSourceFileStream(String centerKey, String productKey, String buildId, String source, String sourceFileName) {
		return fileHelper.getFileStream(s3PathHelper.getBuildSourcesPath(centerKey, productKey, buildId) + source + S3PathHelper.SEPARATOR + sourceFileName);
	}
}
