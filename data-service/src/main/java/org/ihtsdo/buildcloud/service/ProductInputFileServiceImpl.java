package org.ihtsdo.buildcloud.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.service.inputfile.prepare.InputSourceFileProcessor;
import org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.service.security.SecurityHelper;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.otf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static org.ihtsdo.buildcloud.service.inputfile.prepare.ReportType.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
public class ProductInputFileServiceImpl implements ProductInputFileService {

	public static final Logger LOGGER = LoggerFactory.getLogger(ProductInputFileServiceImpl.class);

	private final FileHelper fileHelper;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ProductInputFileDAO dao;

	@Autowired
	private BuildS3PathHelper s3PathHelper;

	@Autowired
	public ProductInputFileServiceImpl(final String buildBucketName, final S3Client s3Client, final S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(buildBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void putManifestFile(String centerKey, final String productKey, final InputStream inputStream, final String originalFilename, final long fileSize) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		dao.putManifestFile(product, inputStream, originalFilename, fileSize);
	}

	@Override
	public String getManifestFileName(String centerKey, final String productKey) throws ResourceNotFoundException {
		StringBuilder manifestDirectoryPathSB = s3PathHelper.getProductManifestDirectoryPath(getProduct(centerKey, productKey));
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String centerKey, final String productKey) throws ResourceNotFoundException {
		return dao.getManifestStream(getProduct(centerKey, productKey));
	}

	@Override
	public void putInputFile(String centerKey, final String productKey, final InputStream inputStream, final String filename, final long fileSize) throws ResourceNotFoundException, IOException {
		Product product = getProduct(centerKey, productKey);
		String productInputFilesPath = s3PathHelper.getProductInputFilesPath(product);
		putFile(filename, inputStream, productInputFilesPath, fileSize);

	}

	@Override
	public InputStream getFileInputStream(String centerKey, final String productKey, final String filename) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return getFileInputStream(product, filename);
	}

	@Override
	public List<String> listInputFilePaths(String centerKey, final String productKey) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return dao.listRelativeInputFilePaths(product);
	}

	@Override
	public void deleteFile(String centerKey, final String productKey, final String filename) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		fileHelper.deleteFile(s3PathHelper.getProductInputFilesPath(product) + filename);
	}

	@Override
	public void deleteFilesByPattern(String centerKey, final String productKey, final String inputFileNamePattern) throws ResourceNotFoundException {
		List<String> filesFound = listInputFilePaths(centerKey, productKey);
		//Need to convert a standard file wildcard to a regex pattern
		String regexPattern = inputFileNamePattern.replace(".", "\\.").replace("*", ".*");
		Pattern pattern = Pattern.compile(regexPattern);
		for (String inputFileName : filesFound) {
			if (pattern.matcher(inputFileName).matches()) {
				deleteFile(centerKey, productKey, inputFileName);
			}
		}
	}

	@Override
	public void putSourceFile(String sourceName, String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException {
		Product product = getProduct(centerKey, productKey);
		if(StringUtils.isBlank(sourceName)) throw new IllegalArgumentException("sourceName cannot be empty");
		String sourceFilesPath = s3PathHelper.getProductSourceSubDirectoryPath(product, sourceName).toString();
		putSourceFile(filename, inputStream, sourceFilesPath, fileSize);

	}

	@Override
	public List<String> listSourceFilePaths(String centerKey, String productKey) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return dao.listRelativeSourceFilePaths(product);
	}

	@Override
	public List<String> listSourceFilePathsFromSubDirectories(String centerKey, String productKey, Set<String> subDirectories) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return dao.listRelativeSourceFilePaths(product, subDirectories);
	}

	@Override
	public List<String> listSourceFilePathsFromSubDirectory(String centerKey, String productKey, String subDirectory) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return dao.listRelativeSourceFilePaths(product, subDirectory);
	}

	@Override
	public void deleteSourceFile(String centerKey, String productKey, String fileName, String subDirectory) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		String filePath;
		if (StringUtils.isBlank(subDirectory)) {
			List<String> paths = listSourceFilePaths(centerKey, productKey);
			for (String path : paths) {
				if(StringUtils.isBlank(fileName) || FilenameUtils.getName(path).equals(fileName)) {
					filePath = s3PathHelper.getProductSourcesPath(product).append(path).toString();
					fileHelper.deleteFile(filePath);
					LOGGER.info("Deleted {} from source directory", filePath);
				}
			}
		} else {
			if (!StringUtils.isBlank(fileName)) {
				filePath = s3PathHelper.getProductSourceSubDirectoryPath(product, subDirectory).append(fileName).toString();
				if (fileHelper.exists(filePath)) {
					fileHelper.deleteFile(filePath);
				} else {
					LOGGER.warn("Could not find {} to delete", filePath);
				}
			} else {
				List<String> toDelete = dao.listRelativeSourceFilePaths(product,subDirectory);
				LOGGER.info("Found total {} files to delete in source folder {} for product {}", toDelete.size(), subDirectory, productKey);
				String sourcePath = s3PathHelper.getProductSourceSubDirectoryPath(product, subDirectory).toString();
				for (String filename : toDelete) {
					LOGGER.debug("Deleting file:" + filename);
					fileHelper.deleteFile(sourcePath + filename);
				}
			}
		}
	}

	@Override
	public void deleteSourceFilesByPattern(String centerKey, String productKey, String inputFileNamePattern, Set<String> subDirectories) throws ResourceNotFoundException {
		String regexPattern = inputFileNamePattern.replace(".", "\\.").replace("*", ".*");
		Pattern pattern = Pattern.compile(regexPattern);
		if (subDirectories != null && !subDirectories.isEmpty()) {
			for (String subDirectory : subDirectories) {
				List<String> filesFound = listSourceFilePathsFromSubDirectory(centerKey, productKey, subDirectory);
				for (String inputFileName : filesFound) {
					if (pattern.matcher(inputFileName).matches()) {
						deleteSourceFile(centerKey, productKey, inputFileName, subDirectory);
					}
				}
			}
		} else {
			List<String> paths = listSourceFilePaths(centerKey, productKey);
			Product product = getProduct(centerKey, productKey);
			for (String path : paths) {
				if(pattern.matcher(FilenameUtils.getName(path)).matches()) {
					String filePath = s3PathHelper.getProductSourcesPath(product).append(path).toString();
					fileHelper.deleteFile(filePath);
					LOGGER.info("Deleted {} from source directory", filePath);
				}
			}
		}
	}

	@Override
	public SourceFileProcessingReport prepareInputFiles(String centerKey, String productKey, boolean copyFilesInManifest) {
		Product product = getProduct(centerKey, productKey);
		SourceFileProcessingReport report = new SourceFileProcessingReport();
		try {
			try (InputStream manifestStream = dao.getManifestStream(product)) {
				if(manifestStream == null) {
					report.add(ERROR, "Failed to load manifest");
				} else {
					//validate manifest.xml
					String validationMsg = ManifestValidator.validate(manifestStream);
					if (validationMsg != null) {
						report.add(ReportType.ERROR, "manifest.xml doesn't conform to the schema definition. " + validationMsg);
					} else {
						try (InputStream manifestInputStream = dao.getManifestStream(product)) {
							InputSourceFileProcessor fileProcessor = new InputSourceFileProcessor(manifestInputStream, fileHelper, s3PathHelper, product, copyFilesInManifest);
							List<String> sourceFiles = listSourceFilePaths(centerKey, productKey);
							if(sourceFiles != null && !sourceFiles.isEmpty()) {
								report = fileProcessor.processFiles(sourceFiles);
							} else {
								report.add(ERROR, "Failed to load files from source directory");
							}
							for (String source : fileProcessor.getSkippedSourceFiles().keySet()) {
								for (String skippedFile : fileProcessor.getSkippedSourceFiles().get(source)) {
									report.add(WARNING, FilenameUtils.getName(skippedFile), null, source, "skipped processing");
								}
							}
						}
					}
				}
			}
		} catch (Throwable t) {
			String errorMsg = String.format("Failed to prepare input files successfully for product %s", productKey);
			LOGGER.error(errorMsg, t);
			report.add(ReportType.ERROR, errorMsg);
		} finally {
			try {
				dao.persistInputPrepareReport(product, report);
			} catch (IOException e) {
				LOGGER.error("Failed to persist input file preparation report!", e);
			}
		}
		return report;
	}

	@Override
	public InputStream getInputPrepareReport(String centerKey, String productKey) throws ResourceNotFoundException {
		Product product = getProduct(centerKey, productKey);
		return dao.getInputPrepareReport(product);
	}

	private InputStream getFileInputStream(final Product product, final String filename) {
		return fileHelper.getFileStream(s3PathHelper.getProductInputFilesPath(product) + filename);
	}

	private Product getProduct(String centerKey,final String productKey) throws ResourceNotFoundException {
		Product product = productDAO.find(centerKey, productKey, SecurityHelper.getRequiredUser());
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
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
	public InputStream getSourceFileStream(String releaseCenterKey, String productKey, String source, String sourceFileName) {
		Product product = getProduct(releaseCenterKey, productKey);
		return fileHelper.getFileStream(s3PathHelper.getProductSourcesPath(product) + source + BuildS3PathHelper.SEPARATOR + sourceFileName);
	}
}
