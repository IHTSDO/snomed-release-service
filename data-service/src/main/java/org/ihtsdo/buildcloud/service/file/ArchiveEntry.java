package org.ihtsdo.buildcloud.service.file;

import java.io.InputStream;

public class ArchiveEntry {
	
	private String filePath;
	private InputStream inputStream;
	
	public ArchiveEntry (String filePath, InputStream inputStream) {
		this.filePath = filePath;
		this.inputStream = inputStream;
	}
	
	public String getFilePath() {
		return filePath;
	}
	public InputStream getInputStream() {
		return inputStream;
	}

}
