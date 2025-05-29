package org.ihtsdo.buildcloud.core.service.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildReport;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.core.service.manager.ReleaseBuildManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

import static org.ihtsdo.buildcloud.core.service.BuildServiceImpl.PROGRESS_STATUS;
import static org.ihtsdo.buildcloud.core.service.BuildServiceImpl.MESSAGE;

@Service
@ConditionalOnProperty(name = "srs.worker", havingValue = "true", matchIfMissing = true)
public class SRSWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReleaseService releaseService;

    @Autowired
    private BuildService buildService;

    @Autowired
    private BuildDAO buildDAO;

    @JmsListener(destination = "${srs.jms.queue.prefix}.build-jobs", concurrency = "${srs.jms.queue.concurrency}")
    public void consumeSRSJob(final TextMessage srsMessage) throws IOException {

        final Instant start = Instant.now();
        CreateReleasePackageBuildRequest buildRequest;
        try {
            buildRequest = objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
        } catch (JMSException | JsonProcessingException e) {
            throw new IllegalStateException("Error occurred while trying to consume the SRS build request message.", e);
        }

        final Build build = buildRequest.getBuild();

        if (build.getId().equals(ReleaseBuildManager.EPOCH_TIME)) {
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(getPreAuthenticatedAuthenticationToken(buildRequest));
        try {
            if (Build.Status.QUEUED != build.getStatus()) {
                throw new IllegalStateException(String.format("Build status expected to be in QUEUED status for the worker to proceed but got %s", build.getStatus().name()));
            }

            if (buildDAO.isBuildCancelRequested(build)) return;

            LOGGER.info("Starting release build: {} for product: {}", build.getId(), build.getProductKey());
            // build status response message is handled by buildDAO
            buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
            releaseService.runReleaseBuild(build);

            final Instant finish = Instant.now();
            LOGGER.info("Release build {} completed in {} minute(s) for product: {}", build.getId(), Duration.between(start, finish).toMinutes(), build.getProductKey());
        } finally {
            if (buildDAO.isBuildCancelRequested(build)) {
                final BuildReport buildReport = getBuildReportFile(build);
                if (buildReport != null) {
                    buildReport.add(PROGRESS_STATUS, "cancelled");
                    buildReport.add(MESSAGE, "Build was cancelled");
                    build.setBuildReport(buildReport);
                    buildDAO.persistReport(build);
                }

                buildDAO.updateStatus(build, Build.Status.CANCELLED);
                buildDAO.deleteOutputFiles(build);
                LOGGER.info("Build has been canceled");
            }
        }
    }

    private PreAuthenticatedAuthenticationToken getPreAuthenticatedAuthenticationToken(CreateReleasePackageBuildRequest buildRequest) {
        final PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken = new PreAuthenticatedAuthenticationToken(buildRequest.getUsername(),
                buildRequest.getAuthenticationToken());
        preAuthenticatedAuthenticationToken.setAuthenticated(true);
        return preAuthenticatedAuthenticationToken;
    }

    private BuildReport getBuildReportFile(final Build build) {
        try (InputStream reportStream = buildService.getBuildReportFile(build)) {
            if (reportStream != null) {
                return objectMapper.readValue(reportStream, BuildReport.class);
            } else {
                LOGGER.warn("No build report file.");
            }
        } catch (IOException e) {
            LOGGER.error("Error occurred while trying to get the build report file.", e);
        }
        return null;
    }
}
