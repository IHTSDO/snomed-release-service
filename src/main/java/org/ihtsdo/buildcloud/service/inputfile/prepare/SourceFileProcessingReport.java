package org.ihtsdo.buildcloud.service.inputfile.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Input file processing report
 *
 */
public class SourceFileProcessingReport {

    private String executionTime;
    private final Map<String, List<String>> soureFiles;
    private final SortedMap<ReportType, List<FileProcessingReportDetail>> details;

    public SourceFileProcessingReport() {
        this.details = new TreeMap<>(new ReportTypeComparator());
        executionTime = new DateTime().toDateTime(DateTimeZone.UTC).toString();
        this.soureFiles = new HashMap<>();
    }

    public Map<ReportType,List<FileProcessingReportDetail>> getDetails() {
        return details;
    }

    public void add(ReportType type, String message) {
        FileProcessingReportDetail detail = new FileProcessingReportDetail(type, message);
        addReportDetail(detail);
    }

    public void add(ReportType type, String fileName, String refsetId, String source, String message) {
        FileProcessingReportDetail detail = new FileProcessingReportDetail(type, fileName, refsetId, source, message);
        addReportDetail(detail);
    }

    public void addReportDetail(FileProcessingReportDetail detail) {
        ReportType type = detail.getType();
		if (this.details.get(type) != null){
           this.details.get(type).add(detail);
        } else {
            List<FileProcessingReportDetail> reports = new ArrayList<>();
            reports.add(detail);
            this.details.put(type, reports);
        }
    }
    
    public void addReportDetails(List<FileProcessingReportDetail> details) {
		for (FileProcessingReportDetail detail : details) {
			addReportDetail(detail);
		}
	}

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }
    
    public Map<String, List<String>> getSoureFiles() {
		return soureFiles;
	}
	
	public void addSoureFiles(String sourceName, List<String> fileList) {
		List<String> fileNameWithoutPath = new ArrayList<>();
		for (String filename : fileList) {
			fileNameWithoutPath.add(FilenameUtils.getName(filename));
		}
		if (this.soureFiles.containsKey(sourceName)) {
			this.soureFiles.get(sourceName).addAll(fileNameWithoutPath);
		} else {
			this.soureFiles.put(sourceName,fileNameWithoutPath);
		}
	}

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            return "Unable to persist Build Report due to " + e.getLocalizedMessage();
        }
    }
    
    static class ReportTypeComparator implements Comparator<ReportType> {
		@Override
		public int compare(ReportType o1, ReportType o2) {
			int result = 0;
		      if (o1.getOrder() > o2.getOrder()) {
		          result = 1;
		      } else if (o1.getOrder() < o2.getOrder()) {
		          result = -1;
		      }
		      return result;
		}
	}
}

