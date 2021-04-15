package org.ihtsdo.buildcloud.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.service.ReleaseServiceImpl;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.jms.TextMessage;

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
	private ReleaseServiceImpl releaseService;

	@JmsListener(destination = "${srs.jms.job.queue}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		LOGGER.info("Build has been picked up from the queue: {}", srsMessage);
		try {
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.RUNNING);
			final CreateReleasePackageBuildRequest createReleasePackageBuildRequest =
					objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
			final Build build = createReleasePackageBuildRequest.getBuild();
			final Product product = build.getProduct();
			releaseService.triggerBuildAsync(product.getReleaseCenter().getBusinessKey(),
					product.getBusinessKey(), build, createReleasePackageBuildRequest.getGatherInputRequestPojo(),
					SecurityContextHolder.getContext().getAuthentication(), createReleasePackageBuildRequest.getRootUrl());
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.COMPLETED);
			LOGGER.info("Build has successfully been executed.");
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.FAILED);
		}
	}
}
