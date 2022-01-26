package org.ihtsdo.buildcloud.core.service.build.compare;

import java.util.Date;

public class ValidationComparisonReport {
    public enum Status {
        RUNNING, PASS, FAILED, FAILED_TO_COMPARE;
    }

    private String compareId;

    private Status status;

    private String message;

    private Date startDate;

    private String leftReportUrl;

    private String rightReportUrl;

    public String getCompareId() {
        return compareId;
    }

    public void setCompareId(String compareId) {
        this.compareId = compareId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getLeftReportUrl() {
        return leftReportUrl;
    }

    public void setLeftReportUrl(String leftReportUrl) {
        this.leftReportUrl = leftReportUrl;
    }

    public String getRightReportUrl() {
        return rightReportUrl;
    }

    public void setRightReportUrl(String rightReportUrl) {
        this.rightReportUrl = rightReportUrl;
    }
}
