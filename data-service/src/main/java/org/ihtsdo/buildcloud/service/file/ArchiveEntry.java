package org.ihtsdo.buildcloud.service.file;

import java.io.InputStream;

public class ArchiveEntry {
	
	private String fileName;
	private InputStream inputStream;
	
	public ArchiveEntry (String fileName, InputStream inputStream) {
		this.fileName = fileName;
		this.inputStream = inputStream;
	}
	
	public String getFileName() {
		return fileName;
	}
	public InputStream getInputStream() {
		return inputStream;
	}

}
