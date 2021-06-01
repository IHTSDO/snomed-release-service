package org.ihtsdo.buildcloud.core.service.inputfile.prepare;
import java.util.HashSet;
import java.util.Set;

public class FileProcessingConfig {
    private final String fileType;
    private final String key;
    private final String targetFileName;
    private Set<String> specificSources;

    public FileProcessingConfig(String fileType, String value, String targetFilename) {
        this.fileType = fileType;
        this.key = value;
        this.targetFileName = targetFilename;
        this.specificSources = new HashSet<>();
    }

    public String getFileType() {
        return fileType;
    }

    public String getKey() {
        return key;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setSpecificSources(Set<String> specificSources) {
        this.specificSources = specificSources;
    }

    public Set<String> getSpecificSources() {
        return specificSources;
    }

    @Override
    public String toString() {
        return "FileProcessingConfig [fileType=" + fileType + ", key=" + key + ", targetFileName=" + targetFileName
                + ", specifiedSources=" + specificSources + "]";
    }


}