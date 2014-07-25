package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.dao.helper.FileHelper;
import org.ihtsdo.buildcloud.dao.helper.S3ClientHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private InputFileDAO dao;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	private FileHelper fileHelper;

	@Autowired
	public InputFileServiceImpl(String executionBucketName, S3Client s3Client, S3ClientHelper s3ClientHelper) {
		fileHelper = new FileHelper(executionBucketName, s3Client, s3ClientHelper);
	}

	@Override
	public void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User authenticatedUser) throws ResourceNotFoundException {
		Package pkg = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		dao.putManifestFile(pkg, inputStream, originalFilename, fileSize);
	}

	@Override
	public String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPath(aPackage);
		List<String> files = fileHelper.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		return dao.getManifestStream(aPackage);
	}
	
	public InputStream getManifestStream(Package pkg) {
		return dao.getManifestStream(pkg);
	}

	@Override
	public void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		String pathPath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileHelper.putFile(inputStream, fileSize, pathPath);
	}
	
	@Override
	public InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		return getFileInputStream(aPackage, filename);
	}
	
	@Override
	public InputStream getFileInputStream(Package pkg, String filename) {
		String filePath = s3PathHelper.getPackageInputFilePath(pkg, filename);
		return fileHelper.getFileStream(filePath);
	}
	

	@Override
	public List<String> listInputFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		return dao.listInputFilePaths(aPackage);
	}

	@Override
	public void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) throws ResourceNotFoundException {
		Package aPackage = getPackage(buildCompositeKey, packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		fileHelper.deleteFile(filePath);
	}

	@Override
	public void deleteFilesByPattern(String buildCompositeKey,
			String packageBusinessKey, String inputFileNamePattern,
			User authenticatedUser) throws ResourceNotFoundException {
		
		List<String> filesFound = listInputFilePaths(buildCompositeKey, packageBusinessKey, authenticatedUser);
		
		//Need to convert a standard file wildcard to a regex pattern
		String regexPattern = inputFileNamePattern.replace(".", "\\.").replace("*", ".*");
		Pattern pattern = Pattern.compile(regexPattern);
		for(String inputFileName : filesFound){
			if(pattern.matcher(inputFileName).matches()){
				deleteFile(buildCompositeKey, packageBusinessKey, inputFileName, authenticatedUser);
			}
		}
	}
	
	private Package getPackage (String buildCompositeKey, String packageBusinessKey, User authenticatedUser) throws ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException ("Unable to find build: " + buildCompositeKey);
		}
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		if (aPackage == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, packageBusinessKey);
			throw new ResourceNotFoundException ("Unable to find package: " +  item);
		}
		return aPackage;
	}

}
