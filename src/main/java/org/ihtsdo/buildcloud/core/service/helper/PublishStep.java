package org.ihtsdo.buildcloud.core.service.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single step in the publish build process.
 */
public class PublishStep {
	public enum StepStatus {
		RUNNING, SUCCESS, FAILED, SKIPPED
	}

	private final String stepName;
	private final int stepNumber;
	private StepStatus status;
	private String errorMessage;
	private List<String> errorDetails;
	private final List<String> warnings;

	public PublishStep(String stepName, int stepNumber) {
		this.stepName = stepName;
		this.stepNumber = stepNumber;
		this.status = StepStatus.RUNNING;
		this.errorMessage = null;
		this.warnings = new ArrayList<>();
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
		if (errorMessage != null) {
			sb.append(", Error: ").append(errorMessage);
		}
		if (!warnings.isEmpty()) {
			sb.append(", Warnings: ").append(String.join("; ", warnings));
		}
		return sb.toString();
	}
}
