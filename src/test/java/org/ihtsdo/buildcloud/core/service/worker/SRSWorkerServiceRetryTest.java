package org.ihtsdo.buildcloud.core.service.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.ReleaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.BEFORE_TRIGGER;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.BUILDING;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.FAILED;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.QUEUED;
import static org.ihtsdo.buildcloud.core.service.BuildServiceImpl.MESSAGE;
import static org.ihtsdo.buildcloud.core.service.BuildServiceImpl.PROGRESS_STATUS;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.RETRY_COUNT;
import static org.ihtsdo.buildcloud.core.dao.helper.S3PathHelper.BUILD_LOG_TXT;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class SRSWorkerServiceRetryTest {

	@AfterEach
	void tearDown() {
		// Defensive cleanup: SRSWorkerService sets authentication on the SecurityContextHolder.
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
	}

	@Test
	void consumeSRSJob_redelivery_midFlight_retriesByCleaningArtifactsAndRunningBuildAgain() throws Exception {
		// Match application ObjectMapper behaviour (see Config#createObjectMapper): ignore unknown fields.
		final ObjectMapper objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		final MocksControl mocks = new MocksControl(MockType.DEFAULT);

		final ReleaseService releaseService = mocks.createMock(ReleaseService.class);
		final BuildService buildService = mocks.createMock(BuildService.class);
		final BuildDAO buildDAO = mocks.createMock(BuildDAO.class);

		final Build build = new Build(new Date(), "international", "product", new BuildConfiguration(), new QATestConfig());
		build.setStatus(BEFORE_TRIGGER);

		// Message payload build (only needs to identify the build); worker reloads authoritative state via DAO.
		final Build messageBuild = new Build(build.getId(), build.getReleaseCenterKey(), build.getProductKey(), BEFORE_TRIGGER.name());
		final CreateReleasePackageBuildRequest request = new CreateReleasePackageBuildRequest(messageBuild, "user", "token");

		final ActiveMQTextMessage message = new ActiveMQTextMessage();
		message.setText(objectMapper.writeValueAsString(request));
		message.setIntProperty("JMSXDeliveryCount", 2); // first redelivery => retryAttempt = 1

		EasyMock.expect(buildDAO.find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), true, true, null, null))
				.andReturn(build);
		EasyMock.expect(buildDAO.isBuildCancelRequested(build)).andReturn(false).anyTimes();

		EasyMock.expect(buildService.getBuildReportFile(build)).andReturn(null);

		buildDAO.cleanupForRetry(build);
		EasyMock.expectLastCall().once();

		buildDAO.persistReport(build);
		EasyMock.expectLastCall().once();

		buildDAO.updateRetryCountMarker(build, 1);
		EasyMock.expectLastCall().once();

		buildDAO.updateStatus(build, QUEUED);
		EasyMock.expectLastCall().once();
		buildDAO.updateStatus(build, BEFORE_TRIGGER);
		EasyMock.expectLastCall().once();

		releaseService.runReleaseBuild(build);
		EasyMock.expectLastCall().once();

		mocks.replay();

		final SRSWorkerService workerService = new SRSWorkerService(objectMapper, releaseService, buildService, buildDAO);
		ReflectionTestUtils.setField(workerService, "interruptedMaxRetries", 3);
		workerService.consumeSRSJob(message);

		mocks.verify();
	}

	@Test
	void consumeSRSJob_redelivery_exceedsMaxRetries_marksBuildFailedAndDoesNotRetry() throws Exception {
		// Match application ObjectMapper behaviour (see Config#createObjectMapper): ignore unknown fields.
		final ObjectMapper objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		final MocksControl mocks = new MocksControl(MockType.DEFAULT);

		final ReleaseService releaseService = mocks.createMock(ReleaseService.class);
		final BuildService buildService = mocks.createMock(BuildService.class);
		final BuildDAO buildDAO = mocks.createMock(BuildDAO.class);

		final Build build = new Build(new Date(), "international", "product", new BuildConfiguration(), new QATestConfig());
		build.setStatus(BUILDING);

		final Build messageBuild = new Build(build.getId(), build.getReleaseCenterKey(), build.getProductKey(), BUILDING.name());
		final CreateReleasePackageBuildRequest request = new CreateReleasePackageBuildRequest(messageBuild, "user", "token");

		final ActiveMQTextMessage message = new ActiveMQTextMessage();
		message.setText(objectMapper.writeValueAsString(request));
		message.setIntProperty("JMSXDeliveryCount", 5); // retryAttempt = 4

		EasyMock.expect(buildDAO.find(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), true, true, null, null))
				.andReturn(build);
		EasyMock.expect(buildDAO.isBuildCancelRequested(build)).andReturn(false).anyTimes();
		EasyMock.expect(buildService.getBuildReportFile(build)).andReturn(null);

		// Existing build log content should be preserved and the report summary appended.
		final ByteArrayOutputStream updatedLog = new ByteArrayOutputStream();
		EasyMock.expect(buildDAO.getLogFileStream(build, BUILD_LOG_TXT))
				.andReturn(new ByteArrayInputStream("existing log\n".getBytes(StandardCharsets.UTF_8)));
		EasyMock.expect(buildDAO.getLogFileOutputStream(build, BUILD_LOG_TXT))
				.andReturn(new org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean(updatedLog, CompletableFuture.completedFuture("ok"), "log/" + BUILD_LOG_TXT));

		buildDAO.persistReport(build);
		EasyMock.expectLastCall().once();

		buildDAO.updateStatus(build, FAILED);
		EasyMock.expectLastCall().once();

		// No cleanupForRetry, no re-queue, no runReleaseBuild expected.
		mocks.replay();

		final SRSWorkerService workerService = new SRSWorkerService(objectMapper, releaseService, buildService, buildDAO);
		ReflectionTestUtils.setField(workerService, "interruptedMaxRetries", 3);
		workerService.consumeSRSJob(message);

		// Build report assertions
		assertNotNull(build.getBuildReport(), "Expected build report to be set when max retries exceeded.");
		assertEquals("failed", String.valueOf(build.getBuildReport().getReport().get(PROGRESS_STATUS)));
		assertEquals(4, ((Number) build.getBuildReport().getReport().get(RETRY_COUNT)).intValue());
		final String reportMessage = String.valueOf(build.getBuildReport().getReport().get(MESSAGE));
		assertTrue(reportMessage.contains("max retries (3)"), "Expected build report message to include max retries. Message=" + reportMessage);
		assertNotNull(build.getBuildReport().getReport().get("lastUpdatedTime"), "Expected lastUpdatedTime in build report.");

		// Log append assertions
		final String updatedLogStr = new String(updatedLog.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(updatedLogStr.contains("existing log"), "Expected original log content to be preserved.");
		assertTrue(updatedLogStr.contains("\"" + MESSAGE + "\""), "Expected build report summary appended to log.");
		assertTrue(updatedLogStr.contains("\"" + RETRY_COUNT + "\""), "Expected retryCount in log summary.");
		assertTrue(updatedLogStr.contains("\"lastUpdatedTime\""), "Expected lastUpdatedTime in log summary.");

		mocks.verify();
	}
}


