package org.ihtsdo.buildcloud.service.helper;

public class ProcessingStatus {
    private String status;
    private String message;

    public ProcessingStatus(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public ProcessingStatus() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
