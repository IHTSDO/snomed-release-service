package org.ihtsdo.buildcloud.core.service.helper;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all steps, status, and error messages during the publish build process.
 * Step lifecycle events are logged so they appear in the SRS build log when a telemetry stream is open.
 */
public class PublishStepTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishStepTracker.class);
	private final List<PublishStep> steps;
	private int nextStepNumber;

	public PublishStepTracker() {
		this.steps = new ArrayList<>();
		this.nextStepNumber = 1;
	}

	/**
	 * Creates and starts a new step with an automatically assigned step number.
	 *
	 * @param stepName the name of the step
	 * @return the created step
	 */
	public PublishStep startStep(String stepName) {
		PublishStep step = new PublishStep(stepName, nextStepNumber);
		steps.add(step);
		nextStepNumber++;
		LOGGER.info("Starting publish step {}: {}", step.getStepNumber(), step.getStepName());
		return step;
	}

	/**
	 * Marks a step as successful.
	 *
	 * @param step the step to mark as successful
	 */
	public void markStepSuccess(PublishStep step) {
		if (step != null) {
			step.finishTiming();
			step.setStatus(PublishStep.StepStatus.SUCCESS);
			LOGGER.info("Completed publish step {}: {} - SUCCESS ({} ms)",
					step.getStepNumber(), step.getStepName(), step.getTimeTakenMillis());
		}
	}

	/**
	 * Marks a step as failed with an error message.
	 *
	 * @param step        the step to mark as failed
	 * @param errorMessage the error message
	 */
	public void markStepFailed(PublishStep step, String errorMessage, List<String> errorDetails) {
		if (step != null) {
			step.finishTiming();
			step.setStatus(PublishStep.StepStatus.FAILED);
			step.setErrorMessage(errorMessage);
			step.setErrorDetails(errorDetails);
			LOGGER.error("Completed publish step {}: {} - FAILED ({} ms): {}",
					step.getStepNumber(), step.getStepName(), step.getTimeTakenMillis(), errorMessage);
		}
	}

	/**
	 * Marks a step as skipped.
	 *
	 * @param step    the step to mark as skipped
	 * @param comment why the step was skipped (shown in API and logs; may be null)
	 */
	public void markStepSkipped(PublishStep step, String comment) {
		if (step != null) {
			step.finishTiming();
			step.setSkipComment(comment);
			step.setStatus(PublishStep.StepStatus.SKIPPED);
			LOGGER.info("Completed publish step {}: {} - SKIPPED ({} ms){}",
					step.getStepNumber(), step.getStepName(), step.getTimeTakenMillis(),
					comment != null && !comment.isEmpty() ? ": " + comment : "");
		}
	}

	/**
	 * Adds a warning to a step.
	 *
	 * @param step    the step to add warning to
	 * @param warning the warning message
	 */
	public void addStepWarning(PublishStep step, String warning) {
		if (step != null) {
			step.addWarning(warning);
		}
	}

	/**
	 * Gets all steps.
	 *
	 * @return list of all steps
	 */
	public List<PublishStep> getSteps() {
		return new ArrayList<>(steps);
	}

	/**
	 * Writes a summary of every publish step to the SRS log.
	 */
	public void logSummary() {
		LOGGER.info("===== PUBLISH STEP SUMMARY ({}) =====", getOverallStatus());
		for (PublishStep step : steps) {
			LOGGER.info("  {}", step);
		}
		LOGGER.info("===== END PUBLISH STEP SUMMARY =====");
	}

	/**
	 * Gets the overall status based on steps and errors.
	 *
	 * @return "RUNNING" if any step is running, "FAILED" if there are errors, "COMPLETED" otherwise
	 */

	@JsonProperty("overallStatus")
	public String getOverallStatus() {
		// Check if any step is still running
		for (PublishStep step : steps) {
			if (step.getStatus() == PublishStep.StepStatus.RUNNING) {
				return "RUNNING";
			}
		}
		// Check if there are any errors
		if (isFailed()) {
			return "FAILED";
		}
		// Otherwise completed
		return "COMPLETED";
	}

	/**
	 * Checks if there are any errors.
	 *
	 * @return true if there are errors, false otherwise
	 */
	private boolean isFailed() {
		return steps.stream().anyMatch(item -> PublishStep.StepStatus.FAILED.equals(item.getStatus()));
	}
}
