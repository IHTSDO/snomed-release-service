package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Autowired
	private ExecutionS3PathHelper s3PathHelper;

	@Override
	public void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);

		// Fist delete any existing manifest files
		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = inputFileDAO.listFiles(directoryPath);
		for (String file : files) {
			inputFileDAO.deleteFile(directoryPath + file);
		}

		// Put new manifest file
		String filePath = manifestDirectoryPathSB.append(originalFilename).toString();
		inputFileDAO.putFile(inputStream, fileSize, filePath);
	}

	@Override
	public String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);
		List<String> files = inputFileDAO.listFiles(manifestDirectoryPathSB.toString());
		if (!files.isEmpty()) {
			return files.iterator().next();
		} else {
			return null;
		}
	}

	@Override
	public InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		StringBuffer manifestDirectoryPathSB = s3PathHelper.getPackageManifestDirectoryPathPath(aPackage);

		String directoryPath = manifestDirectoryPathSB.toString();
		List<String> files = inputFileDAO.listFiles(directoryPath);
		if (!files.isEmpty()) {
			String manifestFilePath = directoryPath + files.iterator().next();
			return inputFileDAO.getFileStream(manifestFilePath);
		} else {
			return null;
		}
	}

	@Override
	public void putFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String pathPath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		inputFileDAO.putFile(inputStream, fileSize, pathPath);
	}

	@Override
	public InputStream getFileStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		return inputFileDAO.getFileStream(filePath);
	}

	@Override
	public List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String directoryPath = s3PathHelper.getPackageInputFilesPath(aPackage);
		return inputFileDAO.listFiles(directoryPath);
	}

	@Override
	public void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser) {
		Package aPackage = packageDAO.find(CompositeKeyHelper.getId(buildCompositeKey), packageBusinessKey, authenticatedUser);
		String filePath = s3PathHelper.getPackageInputFilePath(aPackage, filename);
		inputFileDAO.deleteFile(filePath);
	}

}
