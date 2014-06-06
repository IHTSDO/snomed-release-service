package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.User;

import java.io.InputStream;
import java.util.List;

public interface FileService {

	void putManifestFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String originalFilename, long fileSize, User subject);

	String getManifestFileName(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputStream getManifestStream(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void putFile(String buildCompositeKey, String packageBusinessKey, InputStream inputStream, String filename, long fileSize, User authenticatedUser);

	InputStream getFileStream(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

	List<String> listFilePaths(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	void deleteFile(String buildCompositeKey, String packageBusinessKey, String filename, User authenticatedUser);

}
