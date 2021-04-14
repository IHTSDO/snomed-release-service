package org.ihtsdo.buildcloud.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.TextMessage;

@Service
@ConditionalOnProperty(name = "srs.worker", havingValue = "true", matchIfMissing = true)
public class SRSWorkerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

	@Autowired
	private BuildService buildService;

	@Autowired
	private MessagingHelper messagingHelper;

	@Autowired
	private ActiveMQTextMessage buildStatusTextMessage;

	@Autowired
	private ObjectMapper objectMapper;

	@JmsListener(destination = "${srs.jms.job.queue}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		LOGGER.info("Build has been picked up from the queue: {}", srsMessage);
		try {
			Thread.sleep(2000);
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.RUNNING);
			buildService.triggerBuild(objectMapper.readValue(srsMessage.getText(), Build.class));
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.COMPLETED);
			LOGGER.info("Build has successfully been executed.");
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			messagingHelper.sendResponse(buildStatusTextMessage, SRSWorkerStatus.FAILED);
		}
	}
}
