package org.ihtsdo.buildcloud.core.service.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.*;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.BuildServiceImpl;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.RELEASE_COMPLETE;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.RELEASE_COMPLETE_WITH_WARNINGS;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.*;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
@Transactional
public class BuildStatusListenerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildStatusListenerService.class);
	private static final List<String> RVF_STATUS_MAP_KEYS = Arrays.asList(RUN_ID_KEY, STATE_KEY);
	private static final List<String> RVF_VALIDATION_REQUEST_MAP_KEYS = Arrays.asList(RUN_ID_KEY, BUILD_ID_KEY, RELEASE_CENTER_KEY, PRODUCT_KEY);
	private static final List<String> UPDATE_STATUS_MAP_KEYS = Arrays.asList(RELEASE_CENTER_KEY, PRODUCT_KEY, BUILD_ID_KEY, BUILD_STATUS_KEY);

	@Autowired
	private BuildService buildService;

	@Autowired
	private BuildServiceImpl buildServiceImpl;

	@Autowired
	private ProductService productService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BuildStatusTrackerDao statusTrackerDao;

	@SuppressWarnings("unchecked")
	@JmsListener(destination = "${srs.jms.queue.prefix}.build-job-status")
	public void consumeBuildStatus(final TextMessage textMessage) {
		try {
			if (textMessage != null) {
				final Map<String, Object> message = objectMapper.readValue(textMessage.getText(), Map.class);
				if (propertiesExist(message, RVF_STATUS_MAP_KEYS)) {
					processRVFStatusResponse(message);
				} else if (propertiesExist(message, RVF_VALIDATION_REQUEST_MAP_KEYS)) {
					processSrsWorkerRvfRequest(message);
				} else if (propertiesExist(message, UPDATE_STATUS_MAP_KEYS)) {
					updateStatus(message);
				}
			}
		} catch (JMSException | JsonProcessingException | BadConfigurationException e) {
			LOGGER.error("Error occurred while trying to obtain the build status.", e);
		}
	}

	private boolean propertiesExist(final Map<String, Object> message, final List<String> properties) {
		return properties.stream().allMatch(message::containsKey);
	}

	private void processRVFStatusResponse(final Map<String, Object> message) throws JsonProcessingException, BadConfigurationException {
		final Long runId = (Long) message.get(RUN_ID_KEY);
		LOGGER.info("RVF status response message: {} for run ID: {}", message, runId);
		BuildStatusTracker tracker = statusTrackerDao.findByRvfRunId(String.valueOf(runId));
		if (tracker == null) {
			throw new IllegalStateException("No build status tracker found with RVF run id " + runId);
		}
		final Product product = productService.find(tracker.getReleaseCenterKey(),
				tracker.getProductKey(), true);

		final Build build = buildService.find(product.getReleaseCenter().getBusinessKey(),
				product.getBusinessKey(), tracker.getBuildId(), false,
				false, true, null);

		LOGGER.info("Product: {}, Build: {} for run ID: {}", build.getProductKey(), build.getId(), runId);
		final Build.Status buildStatus = resolveBuildStatusWithResultsFromRvf(message, build, product);
		LOGGER.info("Resolved build status with results from RVF: {}", buildStatus);
		if (buildStatus != null) {
			final BuildReport buildReport = getBuildReportFile(build);
			if (buildReport != null) {
				build.setBuildReport(buildReport);
				buildServiceImpl.setReportStatusAndPersist(build, buildStatus, buildReport, "completed", "Process completed successfully");
			}
			updateStatus(Map.of(RELEASE_CENTER_KEY, product.getReleaseCenter().getBusinessKey(),
					PRODUCT_KEY, product.getBusinessKey(),
					BUILD_ID_KEY, build.getId(),
					BUILD_STATUS_KEY, buildStatus.name()));
		}
	}

	private Build.Status resolveBuildStatusWithResultsFromRvf(final Map<String, Object> message, final Build build, final Product product) {
		final String state = (String) message.get(STATE_KEY);
		switch (state) {
			case "QUEUED":
				return Build.Status.RVF_QUEUED;
			case "RUNNING":
				return Build.Status.RVF_RUNNING;
			case "COMPLETE":
				return processCompleteStatus(build, product);
			case "FAILED":
				return Build.Status.RVF_FAILED;
			default:
				LOGGER.info("Unexpected build status state: {}", state);
				return null;
		}
	}

	private BuildReport getBuildReportFile(final Build build) {
		try (InputStream reportStream = buildService.getBuildReportFile(build)) {
			if (reportStream != null) {
				return objectMapper.readValue(reportStream, BuildReport.class);
			} else {
				LOGGER.warn("No build report file.");
			}
		} catch (IOException e) {
			LOGGER.error("Error occurred while trying to get the build report file.", e);
		}
		return null;
	}

	private Build.Status processCompleteStatus(final Build build, final Product product) {
		build.setPreConditionCheckReports(getPreConditionChecksReport(build, product));

		// Does not check post RVF results.
		boolean hasWarnings = false;
		if (build.getPreConditionCheckReports() != null) {
			hasWarnings = build.getPreConditionCheckReports().stream().anyMatch(conditionCheckReport ->
					conditionCheckReport.getResult() == PreConditionCheckReport.State.WARNING);
		}

		return hasWarnings ? RELEASE_COMPLETE_WITH_WARNINGS : RELEASE_COMPLETE;
	}

	private List<PreConditionCheckReport> getPreConditionChecksReport(final Build build, final Product product) {
		try (InputStream reportStream = buildService.getPreConditionChecksReport(
				product.getReleaseCenter().getBusinessKey(),
				product.getBusinessKey(), build.getId())) {
			if (reportStream != null) {
				return objectMapper.readValue(reportStream, new TypeReference<List<PreConditionCheckReport>>(){});
			} else {
				LOGGER.warn("No pre-condition checks report found.");
			}
		} catch (IOException e) {
			LOGGER.error("Error occurred while trying to get the pre-condition checks report.", e);
		}
		return Collections.emptyList();
	}

	/**
	 * Fires off message to the web socket.
	 *
	 * @param message Being sent to the web socket.
	 */
	private void updateStatus(final Map<String, Object> message) throws JsonProcessingException {
		LOGGER.info("Build status tracker update {}", message);
		final String productBusinessKey = (String) message.get(PRODUCT_KEY);
		final String buildId = (String) message.get(BUILD_ID_KEY);
		final String status = (String) message.get(BUILD_STATUS_KEY);

		BuildStatusTracker tracker = statusTrackerDao.findByProductKeyAndBuildId(productBusinessKey, buildId);
		if (tracker == null) {
			throw new IllegalStateException(String.format("No build status tracker exists for product %s and build id %s", productBusinessKey, buildId));
		}
		String previousStatus = tracker.getStatus();
		Timestamp previousUpdatedTime = tracker.getLastUpdatedTime();
		tracker.setStatus(status);
		statusTrackerDao.update(tracker);
		long timeTakenInMinutes = (tracker.getLastUpdatedTime().getTime() - previousUpdatedTime.getTime())/(1000*60);
		if (previousStatus != null && !previousStatus.equals(status)) {
			LOGGER.info("Status tracking stats for build id {}: It took {} minutes from {} to {}", buildId, timeTakenInMinutes, previousStatus, status);
		}
		if (RELEASE_COMPLETE.name().equals(status) || RELEASE_COMPLETE_WITH_WARNINGS.name().equals(status)) {
			long totalTimeTaken = (tracker.getLastUpdatedTime().getTime() - tracker.getStartTime().getTime())/(1000*60);
			LOGGER.info("Status tracking stats for build id {}: It took {} minutes in total from start to {}", buildId, totalTimeTaken, status);
		}
		LOGGER.info("Web socket status update {}", message);
		simpMessagingTemplate.convertAndSend("/topic/snomed-release-service-websocket", objectMapper.writeValueAsString(message));
	}


	private void processSrsWorkerRvfRequest(final Map<String, Object> message) {
		LOGGER.info("Message from SRS worker for RVF validation request map: {}", message);
		final String buildId = (String) message.get(BUILD_ID_KEY);
		final String productKey = (String) message.get(PRODUCT_KEY);
		final Long rvfRunId = (Long) message.get(RUN_ID_KEY);
		BuildStatusTracker tracker = statusTrackerDao.findByProductKeyAndBuildId(productKey, buildId);
		tracker.setRvfRunId(String.valueOf(rvfRunId));
		statusTrackerDao.update(tracker);
	}
}
