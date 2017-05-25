package org.ihtsdo.buildcloud.service.srs;

import java.util.Set;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 9:36 PM
 */
public class SourceFileMetadata {

    private String fileName;
    private String fileType;
    private String source;
    private Set<TargetFileMetadata> targets;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Set<TargetFileMetadata> getTargets() {
        return targets;
    }

    public void setTargets(Set<TargetFileMetadata> targets) {
        this.targets = targets;
    }
}
