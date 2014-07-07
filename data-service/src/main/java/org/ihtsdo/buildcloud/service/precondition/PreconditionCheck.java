package org.ihtsdo.buildcloud.service.precondition;

import java.util.HashMap;
import java.util.Map;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

public abstract class PreconditionCheck {
	
	public enum State { NOT_RUN("NotRun"), PASS("Pass"), FAIL("Fail");
		private final String value;
		private State (String value) {
			this.value = value;
		}
		public String toString() {
			return this.value;
		}
	}
	
	public enum ResponseKey { PRE_CHECK_NAME("PreCheckName"), RESULT("Result"), MESSAGE("Message");
		private final String value;
		private ResponseKey (String value) {
			this.value = value;
		}
		public String toString() {
			return this.value;
		}
	}
	
	private State state = State.NOT_RUN;
	
	private String responseMessage = "";
	
	protected Execution execution;

	public abstract void runCheck(Package p);
	
	public  Map<ResponseKey, String> getResult() {
		Map<ResponseKey, String> result = new HashMap<ResponseKey, String>();
		result.put(ResponseKey.PRE_CHECK_NAME, getTestName());
		result.put(ResponseKey.RESULT, state.toString());
		result.put(ResponseKey.MESSAGE, getResponseMessage());
		return result;
	}
	
	protected void pass()  {
		this.state = State.PASS;
	}
	
	protected void fail(String msg) {
		this.state = State.FAIL;
		this.responseMessage = msg;
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

	public Execution getExecution() {
		return execution;
	}

	public void setExecution(Execution execution) {
		this.execution = execution;
	}
}
