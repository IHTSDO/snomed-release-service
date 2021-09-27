package org.ihtsdo.buildcloud.core.service.build.compare;

import org.ihtsdo.buildcloud.core.entity.Build;

import java.io.IOException;

public abstract class ComponentComparison {

	private HighLevelComparisonReport.State state = HighLevelComparisonReport.State.NOT_RUN;

	private Object details;

	public abstract void findDiff(Build leftBuild, Build rightBuild) throws IOException;

	protected void pass() {
		this.state = HighLevelComparisonReport.State.PASS;
	}

	protected void pass(Object details) {
		this.state = HighLevelComparisonReport.State.PASS;
		this.details = details;
	}

	protected void fail(Object details) {
		this.state = HighLevelComparisonReport.State.FAILED;
		this.details = details;
	}

	public HighLevelComparisonReport getReport() {
		HighLevelComparisonReport report = new HighLevelComparisonReport();
		report.setTestName(getTestName());
		report.setTestShortName(getTestNameShortname());
		report.setResult(state);
		report.setDetails(this.details);
		return report;
	}

	public String getTestName() {
		return this.getClass().getSimpleName();
	}

	public String getTestNameShortname() {
		return this.getClass().getSimpleName();
	}

	public int getTestOrder() {
		return 0;
	}
}
