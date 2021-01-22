package org.ihtsdo.buildcloud.messaging;

import javax.jms.TextMessage;

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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class BuildTriggerMessageHandler {
	private static final String USERNAME = "username";
	private static final String RVF_FAILURE_EXPORT_MAX = "RvfFailureExportMax";
	public static final String RELEASE_CENTER_KEY = "releaseCenterKey";
	public static final String PRODUCT_KEY = "productKey";
	public static final String BUILD_ID = "buildId";
	public static final String PROPERTY_IS_REQUIRED = " property is required";
	public static final String MRCM_VALIDATION_FORM = "mrcmValidationForm";

	@Autowired
	private BuildService buildService;

	@Autowired
	private MessagingHelper messagingHelper;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@JmsListener(destination = "srs.release-build")
	public void triggerBuild(TextMessage incomingMessage) {
		logger.info("Received message {}", incomingMessage);
		try {
			String authenticationToken = incomingMessage.getStringProperty(MessagingHelper.AUTHENTICATION_TOKEN);
			Assert.notNull(authenticationToken, MessagingHelper.AUTHENTICATION_TOKEN + PROPERTY_IS_REQUIRED);
			String username = incomingMessage.getStringProperty(USERNAME);
			Assert.notNull(username, USERNAME + PROPERTY_IS_REQUIRED);

			PreAuthenticatedAuthenticationToken decoratedAuthentication = new PreAuthenticatedAuthenticationToken(username, authenticationToken);
			SecurityContextHolder.getContext().setAuthentication(decoratedAuthentication);

			final String releaseCenterKey = incomingMessage.getStringProperty(RELEASE_CENTER_KEY);
			Assert.notNull(releaseCenterKey, RELEASE_CENTER_KEY+ PROPERTY_IS_REQUIRED);

			final String productKey = incomingMessage.getStringProperty(PRODUCT_KEY);
			Assert.notNull(productKey, PRODUCT_KEY + PROPERTY_IS_REQUIRED);

			final String buildId = incomingMessage.getStringProperty(BUILD_ID);
			Assert.notNull(buildId, BUILD_ID + PROPERTY_IS_REQUIRED);

			String failureExportMax = incomingMessage.getStringProperty(RVF_FAILURE_EXPORT_MAX);
			Integer exportMax = failureExportMax == null ? null : Integer.valueOf(failureExportMax);

			String mrcmValidationForm = incomingMessage.getStringProperty(MRCM_VALIDATION_FORM);
			QATestConfig.CharacteristicType form = mrcmValidationForm == null ? QATestConfig.CharacteristicType.stated : QATestConfig.CharacteristicType.valueOf(mrcmValidationForm);
			final Build build = buildService.triggerBuild(releaseCenterKey, productKey, buildId, exportMax, form, true);

			messagingHelper.sendResponse(incomingMessage, build);
		} catch (Exception e) {
			messagingHelper.sendErrorResponse(incomingMessage, e);
		}
	}
}
