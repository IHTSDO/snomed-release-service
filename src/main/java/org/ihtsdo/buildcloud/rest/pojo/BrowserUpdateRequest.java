package org.ihtsdo.buildcloud.rest.pojo;

public class BrowserUpdateRequest {

    public enum ImportType {
        DELTA, SNAPSHOT;
    }

    private String releasedProductKey;

    private String releasedBuildKey;

    private ImportType importType;

    public String getReleasedProductKey() {
        return releasedProductKey;
    }

    public void setReleasedProductKey(String releasedProductKey) {
        this.releasedProductKey = releasedProductKey;
    }

    public String getReleasedBuildKey() {
        return releasedBuildKey;
    }

    public void setReleasedBuildKey(String releasedBuildKey) {
        this.releasedBuildKey = releasedBuildKey;
    }

    public ImportType getImportType() {
        return importType;
    }

    public void setImportType(ImportType importType) {
        this.importType = importType;
    }
}
