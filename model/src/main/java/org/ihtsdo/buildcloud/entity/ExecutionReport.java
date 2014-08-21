package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonIgnore;

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

}
