package org.ihtsdo.buildcloud.controller.helper;

import java.util.Map;

import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Splitter;

public class Utils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	
	public static String getFilename (Part file, String defaultFilename){
		String contentDisposition = "";
		try {
			contentDisposition = file.getHeader("content-disposition");
			
			//Content showing 'form-data' which Splitter chokes on due to lack of key/value separator.
			//Unfortunate fix, but strip out for now.
			contentDisposition = contentDisposition.replace("form-data;", "");
			
			Map<String, String> fileAttributes = splitToMap(contentDisposition);
			if (fileAttributes.containsKey("filename")){
				return fileAttributes.get("filename").replace("\"", "").replace(";", "");
			}
		} catch (Exception e) {
			LOGGER.error ("Failed to find filename in content: " + contentDisposition + " returning default filename: " + defaultFilename, e );
		}
		return defaultFilename;
		
	}
	
	private static Map<String, String> splitToMap(String in) {
		return Splitter.on(" ")
						.trimResults()
						.omitEmptyStrings()
						.withKeyValueSeparator("=")
						.split(in);
	}

}
