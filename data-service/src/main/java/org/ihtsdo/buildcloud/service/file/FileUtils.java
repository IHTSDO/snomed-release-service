package org.ihtsdo.buildcloud.service.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
				LOGGER.info (filename + "[" + idx + "]: " + entry.getName());
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

}
