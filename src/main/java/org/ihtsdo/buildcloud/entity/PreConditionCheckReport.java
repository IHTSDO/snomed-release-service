package org.ihtsdo.buildcloud.entity;
/**
 * A status report of pre-condition checks for all packages in an excecution.
 *
 * This entity is stored via S3, not Hibernate.
 */
public class PreConditionCheckReport {
    
    public enum State { NOT_RUN("NotRun"), PASS("Pass"), FAIL("Fail"), WARNING("Warning"),FATAL("Fatal");
	private final String value;
	State(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return this.value;
	}
}
    private String preConditionCheckName;
    private State result;
    private String message;
    
    public String getPreConditionCheckName() {
        return preConditionCheckName;
    }
    public void setPreConditionCheckName(String preConditionCheckName) {
        this.preConditionCheckName = preConditionCheckName;
    }
    public State getResult() {
        return result;
    }
    public void setResult(State result) {
        this.result = result;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
	return "PreConditionCheckReport [preConditionCheckName="
		+ preConditionCheckName + ", result=" + result + ", message="
		+ message + "]";
    }
}
