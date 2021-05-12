package org.ihtsdo.buildcloud.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.service.ReleaseService;
import org.ihtsdo.buildcloud.service.buildstatuslistener.BuildStatusWithProductDetailsRequest;
import org.ihtsdo.buildcloud.service.buildstatuslistener.BuildStatusWithProductDetailsRequest.Builder;
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
		Builder buildStatusWithProductBuilder = null;
		try {
			final Instant start = Instant.now();
			final CreateReleasePackageBuildRequest createReleasePackageBuildRequest =
					objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
			build = createReleasePackageBuildRequest.getBuild();
			final Product product = build.getProduct();
			LOGGER.info("Starting release build: {} for product: {}", build.getId(), product.getName());
			buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
			final PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken =
					new PreAuthenticatedAuthenticationToken(createReleasePackageBuildRequest.getUsername(),
							createReleasePackageBuildRequest.getAuthenticationToken());
			preAuthenticatedAuthenticationToken.setAuthenticated(true);
			SecurityContextHolder.getContext().setAuthentication(preAuthenticatedAuthenticationToken);
			releaseService.runReleaseBuild(product.getReleaseCenter().getBusinessKey(),
					product.getBusinessKey(), build, createReleasePackageBuildRequest.getGatherInputRequestPojo(),
					SecurityContextHolder.getContext().getAuthentication(), createReleasePackageBuildRequest.getRootUrl());
			buildStatusWithProductBuilder = BuildStatusWithProductDetailsRequest.newBuilder(product.getName())
					.withProductBusinessKey(product.getBusinessKey()).withBuildStatus(build.getStatus());
			messagingHelper.sendResponse(buildStatusTextMessage, buildStatusWithProductBuilder.build());
			final Instant finish = Instant.now();
			LOGGER.info("Release build {} completed in {} milliseconds for product: {}", build.getId(), Duration.between(start, finish).toMillis(), product.getId());
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			if (buildDAO != null && buildStatusWithProductBuilder != null) {
				messagingHelper.sendResponse(buildStatusTextMessage, buildStatusWithProductBuilder.withBuildStatus(build.getStatus()).build());
				buildDAO.updateStatus(build, Build.Status.FAILED);
			} else {
				LOGGER.error("Can not update build status to failed due to the BuildDAO being null.");
			}
		}
	}
}
