package org.ihtsdo.buildcloud.service.inputfile.prepare;
import java.util.HashSet;
import java.util.Set;

public class FileProcessingConfig {
    private String fileType;
    private String value;
    private String targetFileName;
    private Set<String> specifiedSources;
    
    public FileProcessingConfig(String fileType, String value, String targetFilename) {
    	this.fileType = fileType;
    	this.value = value;
    	this.targetFileName = targetFilename;
    	this.specifiedSources = new HashSet<>();
    }

    public String getFileType() {
        return fileType;
    }

    public String getValue() {
        return value;
    }

	public String getTargetFileName() {
		return targetFileName;
	}
	
	public void setSpecifiedSources(Set<String> specifiedSources) {
		this.specifiedSources = specifiedSources;
	}

	public Set<String> getSpecifiedSources() {
		return specifiedSources;
	}

	@Override
	public String toString() {
		return "FileProcessingConfig [fileType=" + fileType + ", value=" + value + ", targetFileName=" + targetFileName
				+ ", specifiedSources=" + specifiedSources + "]";
	}

	
}
