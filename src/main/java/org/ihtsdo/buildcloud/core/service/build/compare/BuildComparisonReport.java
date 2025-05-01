package org.ihtsdo.buildcloud.core.service.build.compare;

import java.util.Date;
import java.util.List;

public class BuildComparisonReport {
    public enum Status {
        QUEUED, PUBLISHING, COMPARING, PASSED, FAILED, FAILED_TO_COMPARE;
    }

    private String compareId;

    private String username;

    private String status;

    private String message;

    private Date startDate;

    private String centerKey;

    private String productKey;

    private String leftBuildId;

    private String rightBuildId;

    private boolean readyToPublish;

    private String publishStatus;

    private String publishMessage;

    private List<HighLevelComparisonReport> reports;

    public void setCompareId(String compareId) {
        this.compareId = compareId;
    }

    public String getCompareId() {
        return compareId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setRightBuildId(String rightBuildId) {
        this.rightBuildId = rightBuildId;
    }

    public String getRightBuildId() {
        return rightBuildId;
    }

    public void setLeftBuildId(String leftBuildId) {
        this.leftBuildId = leftBuildId;
    }

    public String getLeftBuildId() {
        return leftBuildId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setReports(List<HighLevelComparisonReport> reports) {
        this.reports = reports;
    }

    public List<HighLevelComparisonReport> getReports() {
        return reports;
    }

    public void setCenterKey(String centerKey) {
        this.centerKey = centerKey;
    }

    public String getCenterKey() {
        return centerKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setReadyToPublish(boolean readyToPublish) {
        this.readyToPublish = readyToPublish;
    }

    public boolean isReadyToPublish() {
        return readyToPublish;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getPublishMessage() {
        return publishMessage;
    }

    public void setPublishMessage(String publishMessage) {
        this.publishMessage = publishMessage;
    }

    public String getPublishStatus() {
        return publishStatus;
    }
}
