package org.ihtsdo.buildcloud.core.service.validation.postcondition;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PostConditionCheckReport;

public abstract class PostconditionCheck {

	private PostConditionCheckReport.State state = PostConditionCheckReport.State.NOT_RUN;

	private String responseMessage = "";

	public abstract void runCheck(Build build);

	public PostConditionCheckReport getReport() {
		PostConditionCheckReport report = new PostConditionCheckReport();
		report.setPostConditionCheckName(getTestName());
		report.setResult(state);
		report.setMessage(getResponseMessage());
		return report;
	}

	protected void pass() {
		this.state = PostConditionCheckReport.State.PASS;
		//need to update this
		this.responseMessage = "";
	}

	protected void fail(String msg) {
		this.state = PostConditionCheckReport.State.FAILED;
		this.responseMessage = msg;
	}

	protected void fatalError(String error) {
		state = PostConditionCheckReport.State.FATAL;
		this.responseMessage = error;
	}

	protected void notRun(String msg) {
		state = PostConditionCheckReport.State.NOT_RUN;
		responseMessage = msg;
	}

	public PostConditionCheckReport.State getState() {
		return state;
	}

	protected void setState(PostConditionCheckReport.State state) {
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
