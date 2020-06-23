package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;

public abstract class PreconditionCheck {

	private State state = State.NOT_RUN;

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
		this.state = State.PASS;
		//need to update this
		this.responseMessage = "";
	}

	protected void fail(String msg) {
		this.state = State.FAIL;
		this.responseMessage = msg;
	}

	protected void fatalError(String error) {
		state = State.FATAL;
		responseMessage = error;
	}

	protected void notRun(String msg) {
		state = State.NOT_RUN;
		responseMessage = msg;
	}

	public State getState() {
		return state;
	}

	protected void setState(State state) {
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
