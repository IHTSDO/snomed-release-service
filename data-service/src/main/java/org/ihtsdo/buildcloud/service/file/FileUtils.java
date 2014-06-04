package org.ihtsdo.buildcloud.service.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

	public static File createDirectoryOrThrow(File directory) throws IOException {
		if (!directory.mkdirs()) {
			throw new IOException("Unable to create directory " + directory.getAbsolutePath());
		}
		return directory;
	}
	
	public static Map<String, String> examineZipContents(String filename, InputStream is){
		//TODO Option to try treating this stream as GZip (GZipInputStream) also.
		Map<String, String> contents = new HashMap<String, String>();
		try {
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry entry = zis.getNextEntry();
			int idx = 0;
			while (entry != null) {
				contents.put("zip_content_" + idx, entry.getName());
				LOGGER.debug (filename + "[" + idx + "]: " + entry.getName());
				entry = zis.getNextEntry();
				idx++;
			}
		} catch (Exception e) {
			LOGGER.debug("Failed to enumerate zip file contents",e);
		}
		
		
		return contents;
	}
	
	public static InputStream[] cloneInputStream(InputStream is) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) > -1 ) {
			baos.write(buffer, 0, len);
		}
		baos.flush();

		// Open new InputStreams using the recorded bytes
		InputStream is1 = new ByteArrayInputStream(baos.toByteArray()); 
		InputStream is2 = new ByteArrayInputStream(baos.toByteArray()); 
		
		return new InputStream[] { is1, is2 };
	}
	
	/**
	 * Modified functionality to add folder as root object ie relative path to the parent.
	 * @param zipFilePath
	 * @param dirToZip
	 * @throws Exception
	 * @author http://www.java2s.com/Code/Java/File-Input-Output/Makingazipfileofdirectoryincludingitssubdirectoriesrecursively.htm
	 */
	public static File zipDir(String zipFilePath, String dirToZip) throws Exception {
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
	public static void addDir(File dirObj, ZipOutputStream out, int parentPathLen) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];
		
		//We also want directories to be represented in the zip file, even if they're empty
		//but no need to do that for the top level directory, so check for that first
		if (dirObj.getAbsolutePath().length() > parentPathLen){
			String relativePath = dirObj.getAbsolutePath().substring(parentPathLen) + File.separator;
			out.putNextEntry(new ZipEntry(relativePath));
		}

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				addDir(files[i], out, parentPathLen);
				continue;
			}

			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			LOGGER.debug(" Adding: " + files[i].getAbsolutePath());
			String relativePath = files[i].getAbsolutePath().substring(parentPathLen);
			out.putNextEntry(new ZipEntry(relativePath));
			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
	}

}
