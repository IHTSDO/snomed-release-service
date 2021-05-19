package org.ihtsdo.buildcloud.service.buildstatuslistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.BuildServiceImpl;
import org.ihtsdo.buildcloud.service.ProductService;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
public class BuildStatusListenerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildStatusListenerService.class);

	private static final String PRODUCT_NAME_KEY = "productName";
	private static final String PRODUCT_KEY = "productKey";
	private static final String RELEASE_CENTER_KEY = "releaseCenterKey";
	private static final String PRODUCT_BUSINESS_KEY = "productBusinessKey";
	private static final String BUILD_STATUS_KEY = "buildStatus";
	private static final String BUILD_ID_KEY = "buildId";
	private static final String RUN_ID_KEY = "runId";
	private static final String STATE_KEY = "state";

	private static final List<String> CONCURRENT_RELEASE_BUILD_MAP_KEYS = Arrays.asList(PRODUCT_NAME_KEY, PRODUCT_BUSINESS_KEY, BUILD_STATUS_KEY);
	private static final List<String> RVF_STATUS_MAP_KEYS = Arrays.asList(RUN_ID_KEY, STATE_KEY);
	private static final List<String> STORE_MINI_RVF_VALIDATION_REQUEST_MAP_KEYS = Arrays.asList(RUN_ID_KEY, BUILD_ID_KEY, RELEASE_CENTER_KEY, PRODUCT_KEY);
	private static final List<String> UPDATE_STATUS_MAP_KEYS = Arrays.asList(RELEASE_CENTER_KEY, PRODUCT_KEY, BUILD_ID_KEY, BUILD_STATUS_KEY);

	private static final Map<String, String> CONCURRENT_RELEASE_BUILD_MAP = new ConcurrentHashMap<>();

	private static final Map<Long, MiniRVFValidationRequest> MINI_RVF_VALIDATION_REQUEST_MAP = new ConcurrentHashMap<>();

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

	@SuppressWarnings("unchecked")
	@JmsListener(destination = "${srs.jms.queue.prefix}.build-job-status")
	public void consumeBuildStatus(final TextMessage textMessage) {
		LOGGER.info("Received message: {}", textMessage);
		try {
			if (textMessage != null) {
				final Map<String, Object> message = objectMapper.readValue(textMessage.getText(), Map.class);
				if (propertiesExist(message, CONCURRENT_RELEASE_BUILD_MAP_KEYS)) {
					processConcurrentReleaseBuildMap(message);
				} else if (propertiesExist(message, RVF_STATUS_MAP_KEYS)) {
					processRVFStatus(message);
				} else if (propertiesExist(message, STORE_MINI_RVF_VALIDATION_REQUEST_MAP_KEYS)) {
					storeMiniRVFValidationRequest(message);
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

	private void processRVFStatus(final Map<String, Object> message) throws JsonProcessingException, BadConfigurationException {
		LOGGER.info("RVF Message: {}", message);
		final MiniRVFValidationRequest miniRvfValidationRequest =
				MINI_RVF_VALIDATION_REQUEST_MAP.get((Long) message.get(RUN_ID_KEY));
		final Product product = productService.find(miniRvfValidationRequest.getReleaseCenterKey(),
				miniRvfValidationRequest.getProductKey(), true);
		final Build build = buildService.find(product.getReleaseCenter().getBusinessKey(),
				product.getBusinessKey(), miniRvfValidationRequest.getBuildId(), true,
				false, true, true);
		final Build.Status buildStatus = getBuildStatusFromRVF(message, build);
		String resultStatus = "completed";
		String resultMessage = "Process completed successfully";
		if (buildStatus != null) {
			final BuildReport report = build.getBuildReport();
			buildServiceImpl.setReportStatusAndPersist(build, buildStatus, report, resultStatus, resultMessage);
			LOGGER.info("SRS Build Status: {}", buildStatus.name());
			updateStatus(ImmutableMap.of(RELEASE_CENTER_KEY, product.getReleaseCenter().getBusinessKey(),
							PRODUCT_KEY, product.getBusinessKey(),
							BUILD_ID_KEY, build.getId(),
							BUILD_STATUS_KEY, buildStatus));
		}
	}

	private Build.Status getBuildStatusFromRVF(final Map<String, Object> message, final Build build) {
		final String state = (String) message.get(STATE_KEY);
		LOGGER.info("State from RVF response: {}", state);
		switch (state) {
			case "QUEUED":
				return Build.Status.RVF_QUEUED;
			case "RUNNING":
				return Build.Status.RVF_RUNNING;
			case "COMPLETE":
				return processCompleteStatus(build);
			case "FAILED":
				return Build.Status.FAILED;
			default:
				LOGGER.info("Unexpected build status state: {}", state);
				return null;
		}
	}

	private Build.Status processCompleteStatus(final Build build) {
		// Does not check post RVF results.
		boolean hasWarnings = false;
		if (build.getPreConditionCheckReports() != null) {
			hasWarnings = build.getPreConditionCheckReports().stream().anyMatch(conditionCheckReport ->
					conditionCheckReport.getResult() == PreConditionCheckReport.State.WARNING);
		}
		return hasWarnings ? Build.Status.RELEASE_COMPLETE_WITH_WARNINGS : Build.Status.RELEASE_COMPLETE;
	}

	private void processConcurrentReleaseBuildMap(final Map<String, Object> message) {
		final Build.Status buildStatus = Build.Status.findBuildStatus((String) message.get(BUILD_STATUS_KEY));
		if (buildStatus != Build.Status.QUEUED && buildStatus != Build.Status.BEFORE_TRIGGER && buildStatus != Build.Status.BUILDING) {
			final String productBusinessKey = (String) message.get(PRODUCT_KEY);
			final String productName = (String) message.get(PRODUCT_NAME_KEY);
			if (productBusinessKey != null && productName != null) {
				CONCURRENT_RELEASE_BUILD_MAP.remove(productBusinessKey, productName);
			}
		}
	}

	/**
	 * Fires off message to the web socket.
	 *
	 * @param message Being sent to the web socket.
	 */
	private void updateStatus(final Map<String, Object> message) throws JsonProcessingException {
		simpMessagingTemplate.convertAndSend("/topic/snomed-release-service-websocket", objectMapper.writeValueAsString(message));
	}

	private void storeMiniRVFValidationRequest(final Map<String, Object> message) {
		MINI_RVF_VALIDATION_REQUEST_MAP.putIfAbsent((Long) message.get(RUN_ID_KEY),
				new MiniRVFValidationRequest((String) message.get(BUILD_ID_KEY),
						(String) message.get(RELEASE_CENTER_KEY),
						(String) message.get(PRODUCT_KEY)));
	}

	/**
	 * Adds the product to the {@code CONCURRENT_RELEASE_BUILD_MAP}. If either
	 * the {@code productBusinessKey} or {@code productName} is null, then the operation will fail gracefully by
	 * exiting the underlying method before the operation has been performed and will
	 * log the relevant message.
	 *
	 * @param productBusinessKey Used as the key entry for the {@code productName} value.
	 * @param productName        Value which will reside in the {@code CONCURRENT_RELEASE_BUILD_MAP}.
	 */
	public final void addProductToConcurrentReleaseBuildMap(final String productBusinessKey, final String productName) {
		if (productBusinessKey == null || productName == null) {
			LOGGER.info("Product business key or product name is null when attempting to add the product to the concurrent release build map.");
			return;
		}
		CONCURRENT_RELEASE_BUILD_MAP.putIfAbsent(productBusinessKey, productName);
	}

	/**
	 * Removes the product from the {@code CONCURRENT_RELEASE_BUILD_MAP}. If either
	 * the {@code productBusinessKey} or {@code productName} is null, then the operation will fail gracefully by
	 * exiting the underlying method before the operation has been performed and will
	 * log the relevant message.
	 *
	 * @param productBusinessKey Used to find the product name so that it can be removed.
	 * @param productName        Value inside the {@code CONCURRENT_RELEASE_BUILD_MAP} which is going
	 *                           to be removed, given the {@code productBusinessKey} exists.
	 */
	public final void removeProductFromConcurrentReleaseBuildMap(final String productBusinessKey, final String productName) {
		if (productBusinessKey == null || productName == null) {
			LOGGER.info("Product business key or product name is null when attempting to remove the product from the concurrent release build map.");
			return;
		}
		CONCURRENT_RELEASE_BUILD_MAP.remove(productBusinessKey, productName);
	}

	/**
	 * Throws {@code EntityAlreadyExistsException} if a build is
	 * already in progress for the given product.
	 *
	 * @param productKey    Used to determine whether the product already
	 *                      exists inside the {@code CONCURRENT_RELEASE_BUILD_MAP}.
	 * @param releaseCenter Used to indicate which product has already been started,
	 *                      in which release center.
	 * @throws EntityAlreadyExistsException If a build is already in progress for
	 *                                      the given product.
	 */
	public final void throwExceptionIfBuildIsInProgressForProduct(final String productKey, final String releaseCenter)
			throws EntityAlreadyExistsException {
		if (productKey != null && CONCURRENT_RELEASE_BUILD_MAP.containsKey(productKey)) {
			throw new EntityAlreadyExistsException("Product " + CONCURRENT_RELEASE_BUILD_MAP.get(productKey) +
					" in release center " + releaseCenter + " has already a in-progress build");
		}
	}
}
