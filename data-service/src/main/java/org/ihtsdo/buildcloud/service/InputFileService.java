package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface InputFileService extends EntityService<InputFile> {

	List<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, User authenticatedUser);

	InputFile find(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey, User authenticatedUser);

	InputFile createUpdate(String buildCompositeKey, String packageBusinessKey, String inputFileName,
						   InputStream fileStream, long fileSize, boolean isManifest, User authenticatedUser) throws IOException;

	InputStream getFileStream(InputFile inputFile) throws IOException;
}
