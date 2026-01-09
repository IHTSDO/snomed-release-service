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

import java.util.Date;

import static org.ihtsdo.buildcloud.core.entity.Build.Status.BEFORE_TRIGGER;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.BUILDING;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.FAILED;
import static org.ihtsdo.buildcloud.core.entity.Build.Status.QUEUED;

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

		buildDAO.persistReport(build);
		EasyMock.expectLastCall().once();

		buildDAO.updateRetryCountMarker(build, 4);
		EasyMock.expectLastCall().once();

		buildDAO.updateStatus(build, FAILED);
		EasyMock.expectLastCall().once();

		// No cleanupForRetry, no re-queue, no runReleaseBuild expected.
		mocks.replay();

		final SRSWorkerService workerService = new SRSWorkerService(objectMapper, releaseService, buildService, buildDAO);
		ReflectionTestUtils.setField(workerService, "interruptedMaxRetries", 3);
		workerService.consumeSRSJob(message);

		mocks.verify();
	}
}


