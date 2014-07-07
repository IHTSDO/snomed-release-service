package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.Package;

import java.io.InputStream;
import java.util.List;

public interface InputFileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User subject);

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(Package pkg);

	void putInputFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser);

	InputStream getFileInputStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	InputStream getFileInputStream(Package pkg, String filename);

	List<String> listInputFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	void deleteFilesByPattern(String buildCompositeKey,
			String packageBusinessKey, String inputFileNamePattern,
			User authenticatedUser);

}
