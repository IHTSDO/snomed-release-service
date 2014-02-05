package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.InputFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface InputFileService {

	Set<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, String authenticatedId);

	InputFile find(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId);

	InputFile create(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey,
					 InputStream fileStream, long fileSize, String authenticatedId) throws IOException;
}
