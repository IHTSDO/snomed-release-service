package org.ihtsdo.buildcloud.core.service.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.easymock.EasyMock;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.BUILD_ID_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.BUILD_STATUS_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.PRODUCT_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.RELEASE_CENTER_KEY;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class BuildStatusListenerServiceRetryAttemptTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BuildStatusTrackerService trackerService;

	@Autowired
	private BuildStatusTrackerDao trackerDao;

	@Test
	void consumeBuildStatus_buildingToQueued_createsNewAttemptRow() throws Exception {
		final String releaseCenterKey = "international";
		final String productKey = "retry_product";
		final String buildId = "2026-01-07T16:47:37";

		BuildStatusTracker attempt0 = new BuildStatusTracker();
		attempt0.setReleaseCenterKey(releaseCenterKey);
		attempt0.setProductKey(productKey);
		attempt0.setBuildId(buildId);
		attempt0.setRetryCount(0);
		attempt0.setStatus(Build.Status.BUILDING.name());
		trackerService.save(attempt0);

		final long id0 = attempt0.getId();
		final Timestamp start0 = attempt0.getStartTime();

		final BuildService buildService = EasyMock.createMock(BuildService.class);

		// Make getRetryCountFromBuildReport return null so listener falls back to increment.
		EasyMock.expect(buildService.find(releaseCenterKey, productKey, buildId, false, false, false, null))
				.andThrow(new RuntimeException("ignore in test"));

		EasyMock.replay(buildService);

		// Use a real SimpMessagingTemplate to avoid EasyMock/CGLIB proxying issues on modern JDKs.
		final MessageChannel noOpChannel = new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				return true;
			}

			@Override
			public boolean send(Message<?> message, long timeout) {
				return true;
			}
		};
		final SimpMessagingTemplate simpMessagingTemplate = new SimpMessagingTemplate(noOpChannel);

		BuildStatusListenerService listener = new BuildStatusListenerService();
		ReflectionTestUtils.setField(listener, "objectMapper", objectMapper);
		ReflectionTestUtils.setField(listener, "trackerService", trackerService);
		ReflectionTestUtils.setField(listener, "buildService", buildService);
		ReflectionTestUtils.setField(listener, "simpMessagingTemplate", simpMessagingTemplate);

		ActiveMQTextMessage msg = new ActiveMQTextMessage();
		msg.setText(objectMapper.writeValueAsString(Map.of(
				RELEASE_CENTER_KEY, releaseCenterKey,
				PRODUCT_KEY, productKey,
				BUILD_ID_KEY, buildId,
				BUILD_STATUS_KEY, Build.Status.QUEUED.name()
		)));

		listener.consumeBuildStatus(msg);

		EasyMock.verify(buildService);

		// Should keep history: a new attempt row is created
		List<BuildStatusTracker> rows = trackerDao.findAllByProductKeyAndBuildId(productKey, buildId);
		assertEquals(2, rows.size());

		BuildStatusTracker latest = rows.get(0); // ordered by start_time desc
		assertNotEquals(id0, latest.getId());
		assertEquals(Build.Status.QUEUED.name(), latest.getStatus());
		assertEquals(1, latest.getRetryCount());
		assertTrue(latest.getStartTime().after(start0));

		BuildStatusTracker original = rows.get(1);
		assertEquals(id0, original.getId());
		assertEquals(Build.Status.BUILDING.name(), original.getStatus());
		assertEquals(0, original.getRetryCount());
	}
}


