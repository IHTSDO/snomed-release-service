package org.ihtsdo.buildcloud.service.srs;

import org.ihtsdo.buildcloud.manifest.RefsetType;

import java.util.List;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 2:22 PM
 */
public class RefsetProcessingConfig {

    private String targetFile;
    private List<RefsetType> refsets;

    public RefsetProcessingConfig(String targetFile, List<RefsetType> refsets) {
        this.targetFile = targetFile;
        this.refsets = refsets;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    public List<RefsetType> getRefsets() {
        return refsets;
    }

    public void setRefsets(List<RefsetType> refsets) {
        this.refsets = refsets;
    }
}
