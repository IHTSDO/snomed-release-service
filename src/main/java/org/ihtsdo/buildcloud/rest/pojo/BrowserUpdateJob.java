package org.ihtsdo.buildcloud.rest.pojo;

public class BrowserUpdateJob {

    public enum JobStatus {
        RUNNING, FAILED, COMPLETED;
    }

    private String jobId;

    private JobStatus status;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private String errorMessage;


}
