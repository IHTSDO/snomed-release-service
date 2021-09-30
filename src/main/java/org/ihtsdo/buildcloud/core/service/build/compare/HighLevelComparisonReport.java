package org.ihtsdo.buildcloud.core.service.build.compare;

public class HighLevelComparisonReport {
    public enum State {
        NOT_RUN("NotRun"), PASS("Pass"), FAILED("Failed");
        private final String value;

        State(String value) {
            this.value = value;
        }
    }

    private String testName;
    private String testShortName;
    private State result;
    private Object details;

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestShortName(String testShortName) {
        this.testShortName = testShortName;
    }

    public String getTestShortName() {
        return testShortName;
    }

    public State getResult() {
        return result;
    }

    public void setResult(State result) {
        this.result = result;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}
