package org.ihtsdo.buildcloud.service.inputfile.prepare;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReportDetail {

    private ReportType type;

    private String fileName;

    private String refsetId;

    private String source;

    private String message;



    public FileProcessingReportDetail(){}

    public FileProcessingReportDetail(ReportType type, String message) {
        this.type = type;
        this.message = message;
    }

    public FileProcessingReportDetail(ReportType type, String fileName, String refsetId, String source, String message) {
        this.type = type;
        this.fileName = fileName;
        this.refsetId = refsetId;
        this.source = source;
        this.message = message;

    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRefsetId() {
        return refsetId;
    }

    public void setRefsetId(String refsetId) {
        this.refsetId = refsetId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
