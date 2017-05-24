package org.ihtsdo.buildcloud.service.srs;

/**
 * User: huyle
 * Date: 5/24/2017
 * Time: 5:38 PM
 */
public class InputFileConfig {

    private String fileType;
    private String header;
    private int processedColumn;

    public InputFileConfig(String fileType, String header, int processedColumn) {
        this.fileType = fileType;
        this.header = header;
        this.processedColumn = processedColumn;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getProcessedColumn() {
        return processedColumn;
    }

    public void setProcessedColumn(int processedColumn) {
        this.processedColumn = processedColumn;
    }
}
