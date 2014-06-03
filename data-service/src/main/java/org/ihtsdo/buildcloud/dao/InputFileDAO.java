package org.ihtsdo.buildcloud.dao;

import java.io.InputStream;
import java.util.List;

public interface InputFileDAO {

	void putFile(InputStream fileStream, long fileSize, String filePath);

	InputStream getFileStream(String pathPath);

	List<String> listFiles(String directoryPath);

	void deleteFile(String filePath);

}
