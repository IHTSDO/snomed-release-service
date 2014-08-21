package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public class ExecutionPackageReport {

	public static final String ERROR_COUNT = "Error Count";
	public static final String FIRST_ERROR = "First Error";
	public static final String FIRST_LINE = "First Line";

	@JsonIgnore
	private Map<String, Object> report;

	ExecutionPackageReport() {
		report = new HashMap<>();
	}

	public void add(String executionStage, String result) {
		report.put(executionStage, result);
	}

	private HashMap<String, Map<String, String>> getDetails(String executionStage) {
		
		//TODO If we already had a straight string here, we could move it into the detail map...
		// Do we have a detail set for this execution stage?
		if (!report.containsKey(executionStage)) {
			report.put(executionStage, new HashMap<String, Map<String, String>>());
		}
		
		return (HashMap<String, Map<String, String>>) report.get(executionStage);
	}

	public void add(String executionStage, String fileName, String problem, int lineNumber) {

		HashMap<String, Map<String, String>> reportDetail = getDetails(executionStage);

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

	public static ExecutionPackageReport getDummyReport() {
		return new ExecutionPackageReport();
	}

	// JsonUnwrapped doesn't work for maps apparently. See https://jira.codehaus.org/browse/JACKSON-765
	@JsonAnyGetter
	public Map<String, Object> getReport() {
		return report;
	}
}
