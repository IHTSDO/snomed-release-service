package org.ihtsdo.buildcloud.core.service.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single step in the publish build process.
 */
public class PublishStep {
	public enum StepStatus {
		RUNNING, SUCCESS, FAILED, SKIPPED
	}

	private final String stepName;
	private final int stepNumber;
	private final long startTimeNanos;
	private Long timeTakenMillis;
	private StepStatus status;
	private String errorMessage;
	private List<String> errorDetails;
	/** When status is SKIPPED, explains why the step was not run. */
	private String skipComment;
	private final List<String> warnings;

	public PublishStep(String stepName, int stepNumber) {
		this.stepName = stepName;
		this.stepNumber = stepNumber;
		this.startTimeNanos = System.nanoTime();
		this.timeTakenMillis = null;
		this.status = StepStatus.RUNNING;
		this.errorMessage = null;
		this.skipComment = null;
		this.warnings = new ArrayList<>();
	}

	/**
	 * Records elapsed wall-clock time for this step (from construction to now). Idempotent.
	 */
	public void finishTiming() {
		if (timeTakenMillis == null) {
			timeTakenMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
		}
	}

	public Long getTimeTakenMillis() {
		return timeTakenMillis;
	}

	public String getStepName() {
		return stepName;
	}

	public int getStepNumber() {
		return stepNumber;
	}

	public StepStatus getStatus() {
		return status;
	}

	public void setStatus(StepStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<String> getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(List<String> errorDetails) {
		this.errorDetails = errorDetails;
	}

	public String getSkipComment() {
		return skipComment;
	}

	public void setSkipComment(String skipComment) {
		this.skipComment = skipComment;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void addWarning(String warning) {
		if (warning != null && !warning.isEmpty()) {
			this.warnings.add(warning);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Step ").append(stepNumber).append(": ").append(stepName)
				.append(" - Status: ").append(status);
		if (timeTakenMillis != null) {
			sb.append(", Time taken: ").append(timeTakenMillis).append(" ms");
		}
		if (errorMessage != null) {
			sb.append(", Error: ").append(errorMessage);
		}
		if (skipComment != null) {
			sb.append(", Skipped: ").append(skipComment);
		}
		if (!warnings.isEmpty()) {
			sb.append(", Warnings: ").append(String.join("; ", warnings));
		}
		return sb.toString();
	}
}
