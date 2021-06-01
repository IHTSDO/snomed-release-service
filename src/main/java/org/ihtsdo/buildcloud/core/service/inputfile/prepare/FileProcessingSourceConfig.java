package org.ihtsdo.buildcloud.core.service.inputfile.prepare;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 9:12 PM
 */
public class FileProcessingSourceConfig {

    private String source;
    private List<String> targetFiles;

    public FileProcessingSourceConfig(String source) {
        this.source = source;
        this.targetFiles = new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getTargetFiles() {
        return targetFiles;
    }

    public void setTargetFiles(List<String> targetFiles) {
        this.targetFiles = targetFiles;
    }
}
