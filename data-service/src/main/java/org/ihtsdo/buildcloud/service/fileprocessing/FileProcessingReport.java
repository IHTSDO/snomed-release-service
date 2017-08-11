package org.ihtsdo.buildcloud.service.fileprocessing;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: huyle
 * Date: 5/29/2017
 * Time: 10:06 AM
 */
public class FileProcessingReport {

    private String executionTime;
    private Map<String,List<FileProcessingReportDetail>> details;


    public FileProcessingReport() {
        this.details = new HashMap<>();
        executionTime = new DateTime().toDateTime(DateTimeZone.UTC).toString();
    }

    public Map<String,List<FileProcessingReportDetail>> getDetails() {
        return details;
    }

    public void add(FileProcessingReportType type, String message) {
        FileProcessingReportDetail detail = new FileProcessingReportDetail(type, message);
        addItemToListFileProcessingReportDetail(type, detail);

    }

    public void add(FileProcessingReportType type,  String fileName, String refsetId, String source, String message) {
        FileProcessingReportDetail detail = new FileProcessingReportDetail(type, fileName, refsetId, source, message);
        addItemToListFileProcessingReportDetail(type, detail);
    }

    private void addItemToListFileProcessingReportDetail(FileProcessingReportType type, FileProcessingReportDetail detail) {
        if(this.details.get(type.name()) == null) {
            this.details.put(type.name(), new ArrayList<FileProcessingReportDetail>());
        }
        this.details.get(type.name()).add(detail);
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public void setDetails(Map<String,List<FileProcessingReportDetail>> details) {
        this.details = details;
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
