package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExecutionReport {

	@JsonIgnore
	private Map<String, ExecutionPackageReport> executionPackageReports;

	@JsonAnyGetter
	public Map<String, ExecutionPackageReport> getExecutionPackageReports() {
		return executionPackageReports;
	}

	public ExecutionPackageReport getExecutionPackgeReport(Package pkg) {
		// Do we already have a map of Execution Report objects per package?
		if (this.executionPackageReports == null) {
			this.executionPackageReports = new HashMap<>();
		}

		// Do we already know about this package?
		if (!this.executionPackageReports.containsKey(pkg.getName())) {
			this.executionPackageReports.put(pkg.getName(), new ExecutionPackageReport());
		}

		return this.executionPackageReports.get(pkg.getName());
	}

	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(executionPackageReports);
		} catch (IOException e) {
			return "Unable to persist Execution Report due to " + e.getLocalizedMessage();
		}
	}

}
