package org.ihtsdo.buildcloud.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

	private static final String MD5_EXTENSION = ".md5";

	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

	public static final String ZIP_EXTENSION = ".zip";

	public static Map<String, String> examineZipContents(final String filename, final InputStream is) {
		//TODO Option to try treating this stream as GZip (GZipInputStream) also.
		Map<String, String> contents = new HashMap<>();
		try {
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry entry = zis.getNextEntry();
			int idx = 0;
			while (entry != null) {
				contents.put("zip_content_" + idx, entry.getName());
				LOGGER.debug(filename + "[" + idx + "]: " + entry.getName());
				entry = zis.getNextEntry();
				idx++;
			}
		} catch (Exception e) {
			LOGGER.debug("Failed to enumerate zip file contents", e);
		}


		return contents;
	}

	/**
	 * Modified functionality to add folder as root object ie relative path to the parent.
	 *
	 * @param zipFilePath
	 * @param dirToZip
	 * @throws Exception
	 * @author http://www.java2s.com/Code/Java/File-Input-Output/Makingazipfileofdirectoryincludingitssubdirectoriesrecursively.htm
	 */
	public static File zipDir(final String zipFilePath, final String dirToZip) throws Exception {
		File dirObj = new File(dirToZip);
		File zipFile = new File(zipFilePath);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		LOGGER.debug("Creating zip file: " + zipFilePath);
		addDir(dirObj, out, dirObj.getAbsolutePath().length());
		out.close();
		return zipFile;
	}

	/*
	 * @param parentPathLength We will deduct this parent path from files/directories put into the zip file
	 */
	public static void addDir(final File dirObj, final ZipOutputStream out, final int parentPathLen) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];

		//We also want directories to be represented in the zip file, even if they're empty
		//but no need to do that for the top level directory, so check for that first
		if (dirObj.getAbsolutePath().length() > parentPathLen) {
			String relativePath = dirObj.getAbsolutePath().substring(parentPathLen) + File.separator;
			out.putNextEntry(new ZipEntry(relativePath));
		}

		for (File file : files) {
			if (file.isDirectory()) {
				addDir(file, out, parentPathLen);
				continue;
			}

			FileInputStream in = new FileInputStream(file.getAbsolutePath());
			LOGGER.debug(" Adding: " + file.getAbsolutePath());
			String relativePath = file.getAbsolutePath().substring(parentPathLen);
			out.putNextEntry(new ZipEntry(relativePath));
			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
	}

	/*
	 *@author http://www.mkyong.com/java/how-to-generate-a-file-checksum-value-in-java/
	 */
	public static String calculateMD5(final File file) throws NoSuchAlgorithmException, IOException {
		StringBuilder sb = new StringBuilder("");
		MessageDigest md = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(file);
		byte[] dataBytes = new byte[1024];

		int bytesRead;
		while ((bytesRead = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, bytesRead);
		}

		//convert the byte to hex format
		byte[] md5Bytes = md.digest();
		for (int i = 0; i < md5Bytes.length; i++) {
			sb.append(Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		fis.close();
		return sb.toString();
	}

	/**
	 * Creates a file in the same directory as hashMe, using the same name with .md5 appended to it.
	 *
	 * @param hashMe
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static File createMD5File(final File hashMe, String md5String) throws NoSuchAlgorithmException, IOException {
		String resultFilePath = hashMe.getAbsolutePath() + MD5_EXTENSION;

		File resultFile = new File(resultFilePath);
		FileOutputStream fop = new FileOutputStream(resultFile);

		// if file doesnt exists, then create it
		if (!resultFile.exists()) {
			resultFile.createNewFile();
		}

		// get the content in bytes
		byte[] contentInBytes = md5String.getBytes();

		fop.write(contentInBytes);
		fop.flush();
		fop.close();

		return resultFile;
	}

	public static boolean hasExtension(final String fileName, final String extension) {
		return fileName.endsWith(extension);
	}

	public static boolean isZip(final String fileName) {
		return hasExtension(fileName, ZIP_EXTENSION);
	}

	public static String getFilenameFromPath(final String filePath) {
		return filePath.substring(filePath.lastIndexOf("/") + 1);
	}
	
	public static boolean isMD5(final String fileName) {
	    return hasExtension(fileName, MD5_EXTENSION);
	}

}
