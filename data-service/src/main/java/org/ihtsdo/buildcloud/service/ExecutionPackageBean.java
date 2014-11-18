package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Package;

public class ExecutionPackageBean {

	private Execution execution;
	private org.ihtsdo.buildcloud.entity.Package aPackage;

	public ExecutionPackageBean(Execution execution, Package aPackage) {
		this.execution = execution;
		this.aPackage = aPackage;
	}

	public ExecutionPackageReport getExecutionPackageReport() {
		return execution.getExecutionReport().getOrCreateExecutionPackgeReport(aPackage);
	}

	public Execution getExecution() {
		return execution;
	}

	public Package getPackage() {
		return aPackage;
	}

}
