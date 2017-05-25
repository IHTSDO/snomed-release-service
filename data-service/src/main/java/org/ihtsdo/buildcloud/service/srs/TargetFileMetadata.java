package org.ihtsdo.buildcloud.service.srs;

/**
 * User: huyle
 * Date: 5/25/2017
 * Time: 9:37 PM
 */
public class TargetFileMetadata {

    private String fileName;
    private String matchedValue;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMatchedValue() {
        return matchedValue;
    }

    public void setMatchedValue(String matchedValue) {
        this.matchedValue = matchedValue;
    }
}
