package org.ihtsdo.buildcloud.service.worker;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.PROPERTY_IS_REQUIRED;

@Service
public class SRSWorkerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

	private static final String BUILD_KEY = "build";

	@Autowired
	private BuildService buildService;

	@Autowired
	private MessagingHelper messagingHelper;

	@JmsListener(destination = "${srs.jms.job.queue}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		LOGGER.info("Product build request message {}", srsMessage);
		try {
			messagingHelper.sendResponse(srsMessage, buildService.triggerBuild((Build) getProperty(srsMessage, BUILD_KEY)));
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			messagingHelper.sendErrorResponse(srsMessage, e);
		}
	}

	/**
	 * Attempts to return the given property from the {@code TextMessage}. If the value does not exist,
	 * it will throw an {@code IllegalArgumentException}.
	 *
	 * @param srsMessage Used to find the given property.
	 * @param message Which is the key to find the property.
	 * @return The given property from thr {@code TextMessage}. If the value does not exist,
	 * it will throw an {@code IllegalArgumentException}.
	 * @throws JMSException If an error occurs while trying to extract the property from the
	 * {@code TextMessage}.
	 */
	private Object getProperty(final TextMessage srsMessage, final String message) throws JMSException {
		final Object property = srsMessage.getObjectProperty(message);
		Assert.notNull(property, message + PROPERTY_IS_REQUIRED);
		return property;
	}
}
