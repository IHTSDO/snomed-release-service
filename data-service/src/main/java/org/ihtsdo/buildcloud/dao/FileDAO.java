package org.ihtsdo.buildcloud.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

public interface FileDAO {

	void putFile(InputStream fileStream, long fileSize, String targetFilePath);

	void putFile(InputStream fileStream, String targetFilePath);

	void putFile(File file, String targetFilePath, boolean calcMD5) throws FileNotFoundException;

	InputStream getFileStream(String pathPath);

	List<String> listFiles(String directoryPath);

	void deleteFile(String filePath);

	void copyFile(String sourcePath, String targetPath);

}
