package org.ihtsdo.buildcloud.core.service.validation.rvf;

public class RVFFailDetail {
    private long failedCount;
    private String details;

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
