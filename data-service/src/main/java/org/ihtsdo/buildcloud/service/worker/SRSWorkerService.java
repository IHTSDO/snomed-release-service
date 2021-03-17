package org.ihtsdo.buildcloud.service.worker;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.otf.jms.MessagingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.*;
import static org.ihtsdo.otf.jms.MessagingHelper.AUTHENTICATION_TOKEN;

@Service
public class SRSWorkerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

	@Autowired
	private BuildService buildService;

	@Autowired
	private MessagingHelper messagingHelper;

	@JmsListener(destination = "${srs.jms.job.queue}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		LOGGER.info("Product build request message {}", srsMessage);
		try {
			SecurityContextHolder.getContext().setAuthentication(
					new PreAuthenticatedAuthenticationToken(getUsernameOrThrowIllegalArgumentException(srsMessage),
							getAuthenticationTokenOrThrowIllegalArgumentException(srsMessage)));

			final Build build = buildService.triggerBuild(getReleaseCenterKeyOrThrowIllegalArgumentException(srsMessage),
					getProductKeyOrThrowIllegalArgumentException(srsMessage), getBuildIdOrThrowIllegalArgumentException(srsMessage),
					getRvfFailureExportMax(srsMessage), getMRCMValidationForm(srsMessage), true);

			messagingHelper.sendResponse(srsMessage, build);
		} catch (final Exception e) {
			LOGGER.error("Error occurred while trying to consume the SRS message.", e);
			messagingHelper.sendErrorResponse(srsMessage, e);
		}
	}

	/**
	 * Returns the MRCM validation form characteristic type. If the property does not exist inside
	 * the {@code TextMessage}, it will return {@code null}.
	 *
	 * @param srsMessage Used to find the given property.
	 * @return The MRCM validation form characteristic type.
	 * @throws JMSException If an error occurs while trying to extract the property from
	 * the {@code TextMessage}.
	 */
	private QATestConfig.CharacteristicType getMRCMValidationForm(final TextMessage srsMessage) throws JMSException {
		final String mrcmValidationForm = srsMessage.getStringProperty(MRCM_VALIDATION_FORM);
		return mrcmValidationForm == null ? QATestConfig.CharacteristicType.stated : QATestConfig.CharacteristicType.valueOf(mrcmValidationForm);
	}

	/**
	 * Returns the RVF failure export max value. If the property does not exist inside
	 * the {@code TextMessage}, it will return {@code null}.
	 *
	 * @param srsMessage Used to find the given property.
	 * @return The RVF failure export max value.
	 * @throws JMSException If an error occurs while trying to extract the property from the
	 * {@code TextMessage}.
	 */
	private Integer getRvfFailureExportMax(final TextMessage srsMessage) throws JMSException {
		final String failureExportMax = srsMessage.getStringProperty(RVF_FAILURE_EXPORT_MAX);
		return failureExportMax == null ? null : Integer.valueOf(failureExportMax);
	}

	private String getBuildIdOrThrowIllegalArgumentException(final TextMessage srsMessage) throws JMSException {
		return getProperty(srsMessage, BUILD_ID);
	}

	private String getProductKeyOrThrowIllegalArgumentException(final TextMessage srsMessage) throws JMSException {
		return getProperty(srsMessage, PRODUCT_KEY);
	}

	private String getReleaseCenterKeyOrThrowIllegalArgumentException(final TextMessage srsMessage) throws JMSException {
		return getProperty(srsMessage, RELEASE_CENTER_KEY);
	}

	private String getUsernameOrThrowIllegalArgumentException(final TextMessage srsMessage) throws JMSException {
		return getProperty(srsMessage, USERNAME);
	}

	private String getAuthenticationTokenOrThrowIllegalArgumentException(final TextMessage srsMessage) throws JMSException {
		return getProperty(srsMessage, AUTHENTICATION_TOKEN);
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
	private String getProperty(final TextMessage srsMessage, final String message) throws JMSException {
		final String property = srsMessage.getStringProperty(message);
		Assert.notNull(property, message + PROPERTY_IS_REQUIRED);
		return property;
	}
}
