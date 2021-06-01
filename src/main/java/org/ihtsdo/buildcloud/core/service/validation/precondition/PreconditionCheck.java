package org.ihtsdo.buildcloud.core.service.validation.precondition;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport;

public abstract class PreconditionCheck {

	private PreConditionCheckReport.State state = PreConditionCheckReport.State.NOT_RUN;

	private String responseMessage = "";

	public abstract void runCheck(Build build);

	public PreConditionCheckReport getReport() {
		PreConditionCheckReport report = new PreConditionCheckReport();
		report.setPreConditionCheckName(getTestName());
		report.setResult(state);
		report.setMessage(getResponseMessage());
		return report;
	}

	protected void pass() {
		this.state = PreConditionCheckReport.State.PASS;
		//need to update this
		this.responseMessage = "";
	}

	protected void fail(String msg) {
		this.state = PreConditionCheckReport.State.FAIL;
		this.responseMessage = msg;
	}

	protected void warning(String msg) {
		this.state = PreConditionCheckReport.State.WARNING;
		this.responseMessage = msg;
	}

	protected void fatalError(String error) {
		state = PreConditionCheckReport.State.FATAL;
		responseMessage = error;
	}

	protected void notRun(String msg) {
		state = PreConditionCheckReport.State.NOT_RUN;
		responseMessage = msg;
	}

	public PreConditionCheckReport.State getState() {
		return state;
	}

	protected void setState(PreConditionCheckReport.State state) {
		this.state = state;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	protected void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	//Default behaviour is to return the class name
	public String getTestName() {
		return this.getClass().getSimpleName();
	}
}
