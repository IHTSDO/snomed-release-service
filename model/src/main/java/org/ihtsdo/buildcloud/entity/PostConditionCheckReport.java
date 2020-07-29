package org.ihtsdo.buildcloud.entity;

/**
 * A status report of post-condition checks for all packages in an execution.
 * <p>
 * This entity is stored via S3, not Hibernate.
 */
public class PostConditionCheckReport {

    public enum State {
        NOT_RUN("NotRun"), PASS("Pass"), FAILED("Failed");
        private final String value;

        private State(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private String postConditionCheckName;
    private State result;
    private String message;

    public String getPostConditionCheckName() {
        return postConditionCheckName;
    }

    public void setPostConditionCheckName(String postConditionCheckName) {
        this.postConditionCheckName = postConditionCheckName;
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
        return "PostConditionCheckReport [postConditionCheckName="
                + postConditionCheckName + ", result=" + result + ", message="
                + message + "]";
    }
}
