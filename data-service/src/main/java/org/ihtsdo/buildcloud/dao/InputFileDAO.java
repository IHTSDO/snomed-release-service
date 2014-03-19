package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.InputFile;

import java.io.InputStream;

public interface InputFileDAO extends EntityDAO<InputFile> {
	InputFile find(Long buildId, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId);

	void save(InputFile inputFile);

	void saveFile(InputStream fileStream, long fileSize, String artifactPath);

	void saveFilePom(InputStream inputStream, int length, String pomPath);

	InputStream getFileStream(InputFile inputFile);

}
