package org.ihtsdo.buildcloud.service.inputfile.gather;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InputGatherReport {

    public enum Status {
        RUNNING, COMPLETED, ERROR
    }

    public class Details {
        String message;
        Status status;

        public Details(Status status, String message) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }

    private String executionTime;
    private Status status;
    private Map<String, Details> details;

    public InputGatherReport() {
        status = Status.RUNNING;
        executionTime = new DateTime().toDateTime(DateTimeZone.UTC).toString();
        details = new HashMap<>();
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Details> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Details> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            return "Unable to persist Sources Gathering Report due to " + e.getLocalizedMessage();
        }
    }

    public void addDetails(Status status, String source, String message) {
        this.details.put(source, new Details(status, message));
    }
}
