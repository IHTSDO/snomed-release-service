package org.ihtsdo.buildcloud.core.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.*;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "srs.worker", havingValue = "true", matchIfMissing = true)
public class SRSWorkerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

	@Autowired
	private MessagingHelper messagingHelper;

	@Autowired
	private ActiveMQTextMessage buildStatusTextMessage;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReleaseService releaseService;

	@Autowired
	private BuildDAO buildDAO;

	@JmsListener(destination = "${srs.jms.queue.prefix}.build-jobs", concurrency = "${srs.jms.queue.concurrency}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		Build build = null;
		Map<String, Object> buildStatusMap = null;
		try {
			final Instant start = Instant.now();
			final CreateReleasePackageBuildRequest createReleasePackageBuildRequest =
					objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
			build = createReleasePackageBuildRequest.getBuild();
			final Product product = build.getProduct();
			buildStatusMap = Map.of(
					PRODUCT_KEY, product.getBusinessKey(),
					RELEASE_CENTER_KEY, product.getReleaseCenter().getBusinessKey(),
					BUILD_ID_KEY, build.getId());

			LOGGER.info("Starting release build: {} for product: {}", build.getId(), product.getName());
			buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
			SecurityContextHolder.getContext().setAuthentication(getPreAuthenticatedAuthenticationToken(createReleasePackageBuildRequest));
			build = releaseService.runReleaseBuild(product.getReleaseCenter().getBusinessKey(),
					product.getBusinessKey(), build, createReleasePackageBuildRequest.getGatherInputRequestPojo(), SecurityContextHolder.getContext().getAuthentication());
			buildStatusMap.put(BUILD_STATUS_KEY, build.getStatus().name());
			LOGGER.info("Sending build Status: {}", buildStatusMap);
			messagingHelper.sendResponse(buildStatusTextMessage, buildStatusMap);
			final Instant finish = Instant.now();
				LOGGER.info("Release build {} completed in {} minute(s) for product: {}", build.getId(), Duration.between(start, finish).toMinutes(), product.getName());
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			if (buildStatusMap != null) {
				buildStatusMap.put(BUILD_STATUS_KEY, Build.Status.FAILED);
				messagingHelper.sendResponse(buildStatusTextMessage, buildStatusMap);
				buildDAO.updateStatus(build, Build.Status.FAILED);
			}
		}
	}

	private PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(CreateReleasePackageBuildRequest buildRequest) {
		final PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = new PreAuthenticatedAuthenticationToken(buildRequest.getUsername(),
						buildRequest.getAuthenticationToken());
		preAuthenticatedAuthenticationToken.setAuthenticated(true);
		return preAuthenticatedAuthenticationToken;
	}
}
