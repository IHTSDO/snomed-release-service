package org.ihtsdo.buildcloud.service.fileprocessing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 9:11 PM
 */
public class FileProcessingConfig {

    private String fileType;
    private String value;
    private Map<String, Set<String>> targetFiles;

    private FileProcessingConfig() {}

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, Set<String>> getTargetFiles() {
        return targetFiles;
    }

    public void setTargetFiles(Map<String, Set<String>> targetFiles) {
        this.targetFiles = targetFiles;
    }

    public void addTargetFileToSource(String source, String fileName) {
        getTargetFiles().get(source).add(fileName);
    }

    public void addTargetFileToAllSources(String fileName) {
        for (Set<String> fileList : targetFiles.values()) {
            fileList.add(fileName);
        }
    }

    public static FileProcessingConfig init(Set<String> availableSources) {
        FileProcessingConfig fileProcessingConfig = new FileProcessingConfig();
        Map<String, Set<String>> defaultTargetFiles = new HashMap<>();
        for (String availableSource : availableSources) {
            defaultTargetFiles.put(availableSource, new HashSet<String>());
        }
        fileProcessingConfig.setTargetFiles(defaultTargetFiles);
        return fileProcessingConfig;
    }

 
}
