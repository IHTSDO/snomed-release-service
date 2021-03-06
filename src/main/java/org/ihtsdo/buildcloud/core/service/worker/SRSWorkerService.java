package org.ihtsdo.buildcloud.core.service.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "srs.worker", havingValue = "true", matchIfMissing = true)
public class SRSWorkerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReleaseService releaseService;

	@Autowired
	private BuildDAO buildDAO;

	@JmsListener(destination = "${srs.jms.queue.prefix}.build-jobs", concurrency = "${srs.jms.queue.concurrency}")
	public void consumeSRSJob(final TextMessage srsMessage) {
		final Instant start = Instant.now();
		CreateReleasePackageBuildRequest buildRequest;
		try {
			buildRequest = objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
		} catch (JMSException | JsonProcessingException e) {
			throw new IllegalStateException("Error occurred while trying to consume the SRS build request message.", e);
		}

		final Build build = buildRequest.getBuild();
		final Product product = build.getProduct();

		LOGGER.info("Starting release build: {} for product: {}", build.getId(), product.getName());
		// build status response message is handled by buildDAO
		buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
		SecurityContextHolder.getContext().setAuthentication(getPreAuthenticatedAuthenticationToken(buildRequest));
		releaseService.runReleaseBuild(product.getReleaseCenter().getBusinessKey(),
				product.getBusinessKey(), build, buildRequest.getGatherInputRequestPojo(), SecurityContextHolder.getContext().getAuthentication());

		final Instant finish = Instant.now();
		LOGGER.info("Release build {} completed in {} minute(s) for product: {}", build.getId(), Duration.between(start, finish).toMinutes(), product.getName());
	}

	private PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(CreateReleasePackageBuildRequest buildRequest) {
		final PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = new PreAuthenticatedAuthenticationToken(buildRequest.getUsername(),
						buildRequest.getAuthenticationToken());
		preAuthenticatedAuthenticationToken.setAuthenticated(true);
		return preAuthenticatedAuthenticationToken;
	}
}
