package org.ihtsdo.buildcloud.entity;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuildReport {

	public static final String ERROR_COUNT = "Error Count";
	public static final String FIRST_ERROR = "First Error";
	public static final String FIRST_LINE = "First Line";

	private final Map<String, Object> report;

	BuildReport() {
		report = new HashMap<>();
	}

	public void add(String buildStage, String result) {
		report.put(buildStage, result);
	}

	private Map<String, Map<String, String>> getDetails(String buildStage) {
		
		//TODO If we already had a straight string here, we could move it into the detail map...
		// Do we have a detail set for this build stage?
		if (!report.containsKey(buildStage)) {
			report.put(buildStage, new HashMap<String, Map<String, String>>());
		}

		@SuppressWarnings("unchecked")
		HashMap<String, Map<String, String>> stringMapHashMap = (HashMap<String, Map<String, String>>) report.get(buildStage);
		return stringMapHashMap;
	}

	public void add(String buildStage, String fileName, String problem, int lineNumber) {

		Map<String, Map<String, String>> reportDetail = getDetails(buildStage);

		// Have we heard about this file before?
		if (!reportDetail.containsKey(fileName)) {
			Map<String, String> fileReport = new HashMap<>();
			reportDetail.put(fileName, fileReport);
			fileReport.put(ERROR_COUNT, Integer.toString(0));
			fileReport.put(FIRST_ERROR, problem);
			fileReport.put(FIRST_LINE, Integer.toString(lineNumber));
		}

		// Increment the error count
		Map<String, String> fileReport = reportDetail.get(fileName);
		String currentErrorCountStr = fileReport.get(ERROR_COUNT);
		int currentErrorCount = Integer.parseInt(currentErrorCountStr);
		fileReport.put(ERROR_COUNT, Integer.toString(++currentErrorCount));
	}

	public static BuildReport getDummyReport() {
		return new BuildReport();
	}

	@JsonAnySetter
	public void setReport(String key, Object value) {
		this.report.put(key, value);
	}

	// JsonUnwrapped doesn't work for maps apparently. See https://jira.codehaus.org/browse/JACKSON-765
	@JsonAnyGetter
	public Map<String, Object> getReport() {
		return report;
	}

	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
		} catch (IOException e) {
			return "Unable to persist Build Report due to " + e.getLocalizedMessage();
		}
	}

}
