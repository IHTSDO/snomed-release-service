package org.ihtsdo.buildcloud.core.service.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildStatusTrackerDao;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.NotificationService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.BUILD_ID_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.BUILD_STATUS_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.PRODUCT_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.PRODUCT_NAME_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.RELEASE_CENTER_KEY;
import static org.ihtsdo.buildcloud.core.service.helper.SRSConstants.RETRY_COUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
	void consumeBuildStatus_retryCountInMessage_createsNewAttemptRowOnce_andWebsocketIncludesRetryCount() throws Exception {
		final String releaseCenterKey = "international";
		final String productKey = "retry_product";
		final String buildId = "2026-01-07T16:47:37";
		final String recipient = "bob";

		BuildStatusTracker attempt0 = new BuildStatusTracker();
		attempt0.setReleaseCenterKey(releaseCenterKey);
		attempt0.setProductKey(productKey);
		attempt0.setBuildId(buildId);
		attempt0.setRetryCount(0);
		attempt0.setStatus(Build.Status.BUILDING.name());
		trackerService.save(attempt0);

		final long id0 = attempt0.getId();
		final Timestamp start0 = attempt0.getStartTime();

		// Use a real SimpMessagingTemplate to avoid EasyMock/CGLIB proxying issues on modern JDKs.
		final List<Message<?>> sentMessages = new ArrayList<>();
		final MessageChannel noOpChannel = new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				sentMessages.add(message);
				return true;
			}

			@Override
			public boolean send(Message<?> message, long timeout) {
				sentMessages.add(message);
				return true;
			}
		};
		final SimpMessagingTemplate simpMessagingTemplate = new SimpMessagingTemplate(noOpChannel);

		// Stub dependencies required for retry notification emission
		final BuildService buildService = mock(BuildService.class);
		final Build buildForRecipient = new Build();
		buildForRecipient.setBuildUser(recipient);
		when(buildService.find(releaseCenterKey, productKey, buildId, false, false, false, null)).thenReturn(buildForRecipient);

		final ProductService productService = mock(ProductService.class);
		final ReleaseCenter rc = new ReleaseCenter();
		rc.setName("International");
		rc.setShortName("International");
		final Product product = new Product("Retry Product");
		rc.addProduct(product);
		when(productService.find(releaseCenterKey, productKey, false)).thenReturn(product);

		final NotificationService notificationService = mock(NotificationService.class);
		when(notificationService.create(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BuildStatusListenerService listener = new BuildStatusListenerService();
		ReflectionTestUtils.setField(listener, "objectMapper", objectMapper);
		ReflectionTestUtils.setField(listener, "trackerService", trackerService);
		ReflectionTestUtils.setField(listener, "simpMessagingTemplate", simpMessagingTemplate);
		ReflectionTestUtils.setField(listener, "buildService", buildService);
		ReflectionTestUtils.setField(listener, "productService", productService);
		ReflectionTestUtils.setField(listener, "notificationService", notificationService);

		ActiveMQTextMessage msg = new ActiveMQTextMessage();
		msg.setText(objectMapper.writeValueAsString(Map.of(
				RELEASE_CENTER_KEY, releaseCenterKey,
				PRODUCT_KEY, productKey,
				BUILD_ID_KEY, buildId,
				BUILD_STATUS_KEY, Build.Status.QUEUED.name(),
				RETRY_COUNT, 1
		)));

		listener.consumeBuildStatus(msg);

		// Websocket: should send the normal status change message including retryCount from the status update.
		assertTrue(sentMessages.size() >= 1, "Expected at least 1 websocket message (status-change).");
		assertTrue(sentMessages.stream().anyMatch(m -> ("/topic/user/" + recipient + "/notification").equals(getDestination(m.getHeaders()))),
				"Expected websocket NEW_NOTIFICATION event to be sent to the recipient channel.");

		// Notification: should be created once and addressed to the build trigger user
		ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationService, times(1)).create(notifCaptor.capture());
		assertEquals(recipient, notifCaptor.getValue().getRecipient());
		assertEquals(Notification.NotificationType.BUILD_RETRIED.name(), notifCaptor.getValue().getNotificationType());
		@SuppressWarnings("unchecked")
		Map<String, Object> notifDetails = objectMapper.readValue(notifCaptor.getValue().getDetails(), Map.class);
		assertEquals("Retry Product", notifDetails.get(PRODUCT_NAME_KEY));
		assertTrue(String.valueOf(notifDetails.get("message")).contains("Retry Product"),
				"Expected retry notification message to include product name.");
		Message<?> statusChange = sentMessages.stream()
				.filter(m -> "/topic/build-status-change".equals(getDestination(m.getHeaders())))
				.findFirst()
				.orElse(null);
		assertNotNull(statusChange, "Expected websocket status-change message to be sent.");
		@SuppressWarnings("unchecked")
		Map<String, Object> payload = objectMapper.readValue(String.valueOf(statusChange.getPayload()), Map.class);
		assertEquals(1, ((Number) payload.get(RETRY_COUNT)).intValue());

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

		// Sending the same retryCount again should not create another attempt row
		ActiveMQTextMessage msg2 = new ActiveMQTextMessage();
		msg2.setText(objectMapper.writeValueAsString(Map.of(
				RELEASE_CENTER_KEY, releaseCenterKey,
				PRODUCT_KEY, productKey,
				BUILD_ID_KEY, buildId,
				BUILD_STATUS_KEY, Build.Status.BEFORE_TRIGGER.name(),
				RETRY_COUNT, 1
		)));
		listener.consumeBuildStatus(msg2);

		List<BuildStatusTracker> rowsAfterSecond = trackerDao.findAllByProductKeyAndBuildId(productKey, buildId);
		assertEquals(2, rowsAfterSecond.size(), "Expected no additional retry attempt row for same retryCount.");
		// No extra notifications for same retryCount
		verify(notificationService, times(1)).create(any(Notification.class));
	}

	private String getDestination(MessageHeaders headers) {
		Object dest = headers.get("simpDestination");
		return dest == null ? null : String.valueOf(dest);
	}
}


