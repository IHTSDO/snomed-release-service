package org.ihtsdo.buildcloud.dao;

import java.io.InputStream;
import java.util.List;

public interface FileDAO {

	void putFile(InputStream fileStream, long fileSize, String filePath);

	void putFile(InputStream fileStream, String filePath);

	InputStream getFileStream(String pathPath);

	List<String> listFiles(String directoryPath);

	void deleteFile(String filePath);

	void copyFile(String sourcePath, String targetPath);

}
