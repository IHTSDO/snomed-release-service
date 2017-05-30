package org.ihtsdo.buildcloud.service.fileprocessing;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReportDetail {

    private FileProcessingReportType type;

    private String message;

    public FileProcessingReportDetail(FileProcessingReportType type, String message) {
        this.type = type;
        this.message = message;
    }

    public FileProcessingReportType getType() {
        return type;
    }

    public void setType(FileProcessingReportType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
