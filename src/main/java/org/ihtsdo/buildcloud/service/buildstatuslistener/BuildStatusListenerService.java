package org.ihtsdo.buildcloud.service.buildstatuslistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.service.worker.BuildStatus;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
public class BuildStatusListenerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuildStatusListenerService.class);

	private static final Map<String, String> CONCURRENT_RELEASE_BUILD_MAP = new ConcurrentHashMap<>();

	@Autowired
	private ObjectMapper objectMapper;

	@JmsListener(destination = "${srs.build.job.status.queue}")
	public void consumeBuildStatus(final TextMessage textMessage) {
		try {
			final BuildStatusWithProductDetailsRequest buildStatusWithProductDetailsRequest =
					objectMapper.readValue(textMessage.getText(), BuildStatusWithProductDetailsRequest.class);
			if (buildStatusWithProductDetailsRequest != null) {
				final BuildStatus buildStatus = buildStatusWithProductDetailsRequest.getBuildStatus();
				if (buildStatus == BuildStatus.COMPLETED || buildStatus == BuildStatus.FAILED) {
					final String productBusinessKey = buildStatusWithProductDetailsRequest.getProductBusinessKey();
					final String productName = buildStatusWithProductDetailsRequest.getProductName();
					if (productBusinessKey != null && productName != null) {
						CONCURRENT_RELEASE_BUILD_MAP.remove(productBusinessKey, productName);
					}
				}
			}
		} catch (JsonProcessingException | JMSException e) {
			LOGGER.error("Error occurred while trying to obtain the build status.", e);
		}
	}

	/**
	 * Adds the product to the {@code CONCURRENT_RELEASE_BUILD_MAP}. If either
	 * the {@code productBusinessKey} or {@code productName} is null, then the operation will fail gracefully by
	 * exiting the underlying method before the operation has been performed and will
	 * log the relevant message.
	 *
	 * @param productBusinessKey Used as the key entry for the {@code productName} value.
	 * @param productName Value which will reside in the {@code CONCURRENT_RELEASE_BUILD_MAP}.
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
	 * @param productName Value inside the {@code CONCURRENT_RELEASE_BUILD_MAP} which is going
	 *                    to be removed, given the {@code productBusinessKey} exists.
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
	 * @param productKey Used to determine whether the product already
	 *                   exists inside the {@code CONCURRENT_RELEASE_BUILD_MAP}.
	 * @param releaseCenter Used to indicate which product has already been started,
	 *                      in which release center.
	 * @throws EntityAlreadyExistsException If a build is already in progress for
	 * the given product.
	 */
	public final void throwExceptionIfBuildIsInProgressForProduct(final String productKey, final String releaseCenter)
			throws EntityAlreadyExistsException {
		if (productKey != null && CONCURRENT_RELEASE_BUILD_MAP.containsKey(productKey)) {
			throw new EntityAlreadyExistsException("Product " + CONCURRENT_RELEASE_BUILD_MAP.get(productKey) +
					" in release center " + releaseCenter + " has already a in-progress build");
		}
	}
}
