package org.ihtsdo.buildcloud.service.fileprocessing;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReport {

    private String executionTime;
    private List<FileProcessingReportDetail> details;


    public FileProcessingReport() {
        this.details = new ArrayList<>();
        executionTime = new DateTime().toDateTime(DateTimeZone.UTC).toString();
    }

    public List<FileProcessingReportDetail> getDetails() {
        return details;
    }

    public void add(FileProcessingReportType type, String message) {
        FileProcessingReportDetail detail = new FileProcessingReportDetail(type, message);
        this.details.add(detail);
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            return "Unable to persist Build Report due to " + e.getLocalizedMessage();
        }
    }
}
