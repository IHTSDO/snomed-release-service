package org.ihtsdo.buildcloud.core.service.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildReport;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.ihtsdo.buildcloud.core.service.manager.ReleaseBuildManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.ihtsdo.buildcloud.core.service.BuildServiceImpl.*;
import static org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper.BUILD_LOG_TXT;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.RETRY_COUNT;

@Service
@ConditionalOnProperty(name = "srs.worker", havingValue = "true", matchIfMissing = true)
public class SRSWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SRSWorkerService.class);
	private static final String LAST_UPDATED_TIME = "lastUpdatedTime";

    private final ObjectMapper objectMapper;

    private final ReleaseService releaseService;

    private final BuildService buildService;

    private final BuildDAO buildDAO;

	@Value("${srs.build.interrupted.max-retries:3}")
	private int interruptedMaxRetries;

    @Autowired
    public SRSWorkerService(ObjectMapper objectMapper, ReleaseService releaseService, BuildService buildService, BuildDAO buildDAO) {
        this.objectMapper = objectMapper;
        this.releaseService = releaseService;
        this.buildService = buildService;
        this.buildDAO = buildDAO;

    }

    @JmsListener(destination = "${srs.jms.queue.prefix}.build-jobs", concurrency = "${srs.jms.queue.concurrency}")
    public void consumeSRSJob(final TextMessage srsMessage) throws IOException {
		final Instant start = Instant.now();
		CreateReleasePackageBuildRequest buildRequest;
		try {
			buildRequest = objectMapper.readValue(srsMessage.getText(), CreateReleasePackageBuildRequest.class);
		} catch (JMSException | JsonProcessingException e) {
			throw new IllegalStateException("Error occurred while trying to consume the SRS build request message.", e);
		}

		final Build messageBuild = buildRequest.getBuild();

		if (messageBuild.getId().equals(ReleaseBuildManager.EPOCH_TIME)) {
			return;
		}

		// Reload the authoritative build state from storage
		Build build = buildDAO.find(
				messageBuild.getReleaseCenterKey(),
				messageBuild.getProductKey(),
				messageBuild.getId(),
				true,
				true,
				null,
				null);

		if (build == null) {
			LOGGER.warn("Build not found for releaseCenterKey {}, productKey {}, buildId {}. Ignoring message.",
					messageBuild.getReleaseCenterKey(), messageBuild.getProductKey(), messageBuild.getId());
			return;
		}

		final Build.Status status = build.getStatus();
        final int deliveryCount = getDeliveryCount(srsMessage);

		SecurityContextHolder.getContext().setAuthentication(getPreAuthenticatedAuthenticationToken(buildRequest));
		try {
			if (Build.Status.PENDING == status || Build.Status.QUEUED == status) {
				// First (or clean) attempt: run as normal
				if (buildDAO.isBuildCancelRequested(build)) {
					return;
				}

				LOGGER.info("Starting release build: {} for product: {}", build.getId(), build.getProductKey());
				// build status response message is handled by buildDAO
				buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
				releaseService.runReleaseBuild(build);

				final Instant finish = Instant.now();
				LOGGER.info("Release build {} completed in {} minute(s) for product: {}",
						build.getId(), Duration.between(start, finish).toMinutes(), build.getProductKey());
			} else if (deliveryCount > 1 && (Build.Status.BEFORE_TRIGGER == status || Build.Status.BUILDING == status)) {
				// If we're seeing a build mid-flight, only retry if this is a redelivery (i.e. prior attempt likely died before ack due to spot instances)
                retryInterruptedBuild(build, start, deliveryCount - 1);
			}
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


	private void retryInterruptedBuild(Build build, Instant start, int retryAttempt) throws IOException {
		try {
			if (buildDAO.isBuildCancelRequested(build)) {
				return;
			}

			// retryAttempt is 1 for first redelivery, 2 for second redelivery, etc.
			if (retryAttempt > interruptedMaxRetries) {
				LOGGER.warn("Max retries ({}) exceeded for interrupted build {} (attempt={}); marking FAILED.",
						interruptedMaxRetries, build.getUniqueId(), retryAttempt);
				final String failMessage = String.format(
						"Build was interrupted and max retries (%d) were reached. Marking as FAILED.",
						interruptedMaxRetries);
				final BuildReport report = buildReportWithRetryCount(build, retryAttempt,
						"failed",
						failMessage);
				build.setBuildReport(report);
				buildDAO.persistReport(build);
				appendBuildReportSummaryToEndOfBuildLog(build, report);
				// Do not persist/send retryCount when max retries is exceeded.
				// This prevents downstream "retried" notifications and avoids reporting a new attempt when we're not retrying.
				buildDAO.updateStatus(build, Build.Status.FAILED);
				return;
			}

			// Remove partial artifacts from the prior attempt; keep only manifest + config files
			buildDAO.cleanupForRetry(build);

			final BuildReport report = buildReportWithRetryCount(build, retryAttempt,
					"retrying",
					"Retrying after interruption. Partial build artifacts were deleted before retry.");
			build.setBuildReport(report);
			buildDAO.persistReport(build);
			buildDAO.updateRetryCountMarker(build, retryAttempt);
			// Ensure subsequent status update messages include retryCount without requiring a reload from storage.
			build.setRetryCount(retryAttempt);

			// Reset status back to QUEUED then run again as a clean attempt
			buildDAO.updateStatus(build, Build.Status.QUEUED);

			if (buildDAO.isBuildCancelRequested(build)) {
				return;
			}
			LOGGER.info("Starting (retry) release build: {} for product: {}", build.getId(), build.getProductKey());
			buildDAO.updateStatus(build, Build.Status.BEFORE_TRIGGER);
			releaseService.runReleaseBuild(build);

			final Instant finish = Instant.now();
			LOGGER.info("Release build {} (retry) completed in {} minute(s) for product: {}",
					build.getId(), Duration.between(start, finish).toMinutes(), build.getProductKey());
		} catch (Exception e) {
			// Log but do not rethrow, to avoid endless redelivery loops
			LOGGER.error("Failed to retry interrupted build {}.", build.getUniqueId(), e);
            buildDAO.updateStatus(build, Build.Status.FAILED);
		}
	}


    private int getDeliveryCount(TextMessage message) {
        try {
            if (message.propertyExists("JMSXDeliveryCount")) {
				// ActiveMQ commonly sets JMSXDeliveryCount (1 on first delivery, 2 on first redelivery, etc.)
				// Not guaranteed across all brokers/configurations; keep a safe fallback.
                return Math.max(1, message.getIntProperty("JMSXDeliveryCount"));
            }
            LOGGER.info("JMSXDeliveryCount is not set and use getJMSRedelivered() as fallback");
            return message.getJMSRedelivered() ? 2 : 1; // best-effort fallback
        } catch (JMSException e) {
            return 1;
        }
    }

	private BuildReport buildReportWithRetryCount(Build build, int retryCount, String progressStatus, String message) {
		BuildReport report = getBuildReportFile(build);
		if (report == null) {
			report = BuildReport.getDummyReport();
		}
		report.add(PROGRESS_STATUS, progressStatus);
		report.add(MESSAGE, message);
		report.setReport(LAST_UPDATED_TIME, Instant.now().toString());
		report.setReport(RETRY_COUNT, retryCount);
		return report;
	}

	private void appendBuildReportSummaryToEndOfBuildLog(final Build build, final BuildReport report) {
		// Best-effort only: this must never block or break the worker flow.
		if (build == null) {
			LOGGER.warn("Cannot append build report summary to build log: build is null.");
			return;
		}
		if (report == null || report.getReport() == null) {
			LOGGER.warn("Cannot append build report summary to build log for build {}: report is null.",
					build.getUniqueId());
			return;
		}

		final Map<String, Object> reportMap = report.getReport();
		final Map<String, Object> summaryMap = new java.util.LinkedHashMap<>();
		summaryMap.put(MESSAGE, reportMap.get(MESSAGE));
		summaryMap.put(RETRY_COUNT, reportMap.get(RETRY_COUNT));
		summaryMap.put(PROGRESS_STATUS, reportMap.get(PROGRESS_STATUS));
		summaryMap.put(LAST_UPDATED_TIME, reportMap.get(LAST_UPDATED_TIME));

		final byte[] summaryBytes;
		try {
			final String summaryJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summaryMap);
			final String summary = "\n\n"
					+ "===== BUILD REPORT UPDATE =====\n"
					+ summaryJson + "\n"
					+ "===============================\n";
			summaryBytes = summary.getBytes(StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.warn("Failed to serialize build report summary to JSON for build {}.", build.getUniqueId(), e);
			return;
		}

		AsyncPipedStreamBean logOutputStreamBean = null;
		try (InputStream existingLog = buildDAO.getLogFileStream(build, BUILD_LOG_TXT)) {
			logOutputStreamBean = buildDAO.getLogFileOutputStream(build, BUILD_LOG_TXT);
			try (OutputStream logOutputStream = logOutputStreamBean.getOutputStream()) {
				if (existingLog != null) {
					IOUtils.copy(existingLog, logOutputStream);
				}
				logOutputStream.write(summaryBytes);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to append build report summary to build log for build {}.", build.getUniqueId(), e);
		} finally {
			// Important: wait only after the stream is closed, otherwise the async upload may block waiting for EOF.
			if (logOutputStreamBean != null) {
				try {
					logOutputStreamBean.waitForFinish();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					LOGGER.error("Interrupted while waiting for build log append upload to finish for build {}.", build.getUniqueId(), ie);
				} catch (Exception e) {
					LOGGER.error("Failed while waiting for build log append upload to finish for build {}.", build.getUniqueId(), e);
				}
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
