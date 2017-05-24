package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
		if (filename.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			Path tempFile = Files.createTempFile(getClass().getCanonicalName(), ".zip");
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					String fileDestinationPath = productInputFilesPath + FileUtils.getFilenameFromPath(entry.getName());
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
			String fileDestinationPath = productInputFilesPath + filename;
			fileHelper.putFile(inputStream, fileSize, fileDestinationPath);
		}
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

	@Override
	public void putSourceFile(String sourceName, String centerKey, String productKey, InputStream inputStream, String filename, long fileSize) throws ResourceNotFoundException, IOException {
		Product product = getProduct(centerKey, productKey);

		String sourceFilesPath = s3PathHelper.getProductSourcePath(product);
		if (filename.endsWith(RF2Constants.ZIP_FILE_EXTENSION)) {
			Path tempFile = Files.createTempFile(getClass().getCanonicalName(), ".zip");
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					String fileDestinationPath = sourceFilesPath + FileUtils.getFilenameFromPath(entry.getName());
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
			String fileDestinationPath = sourceFilesPath + filename;
			fileHelper.putFile(inputStream, fileSize, fileDestinationPath);
		}
	}
}
