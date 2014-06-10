package org.ihtsdo.buildcloud.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.codec.DecoderException;

public interface FileDAO {

	void putFile(InputStream fileStream, long fileSize, String targetFilePath);

	void putFile(InputStream fileStream, String targetFilePath);

	String putFile(File file, String targetFilePath, boolean calcMD5) throws FileNotFoundException, NoSuchAlgorithmException, IOException, DecoderException;

	InputStream getFileStream(String pathPath);

	List<String> listFiles(String directoryPath);

	void deleteFile(String filePath);

	void copyFile(String sourcePath, String targetPath);

}
