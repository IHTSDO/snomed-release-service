package org.ihtsdo.buildcloud.core.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional()
public class NotificationDaoImplTest {

	@Autowired
	private NotificationDao notificationDao;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void testCRUD() throws JsonProcessingException {
		Notification notification = new Notification();
		notification.setRecipient("test");
		notification.setNotificationType(Notification.NotificationType.BUILD_RUN_OUT_OF_TIME.name());
		notification.setRead(false);

		Map<String, String> details = new HashMap<>();
		details.put("key1", "value1");
		details.put("key2", "value2");
		notification.setDetails(objectMapper.writeValueAsString(details));

		notificationDao.save(notification);

		Notification result = notificationDao.load(notification.getId());
		assertNotNull(result);
		assertEquals("test", result.getRecipient());
		assertEquals(Notification.NotificationType.BUILD_RUN_OUT_OF_TIME.name(), result.getNotificationType());
		assertFalse(result.isRead());

		Map<String, String> resultDetails = objectMapper.readValue(result.getDetails(), HashMap.class);
		assertTrue(resultDetails.containsKey("key1"));
		assertTrue(resultDetails.containsKey("key2"));
		assertEquals("value1", resultDetails.get("key1"));
		assertEquals("value2", resultDetails.get("key2"));
		result.setRead(true);
		notificationDao.update(result);

		result = notificationDao.load(notification.getId());
		assertNotNull(result);
		assertEquals("test", result.getRecipient());
		assertTrue(result.isRead());

		notificationDao.delete(result);
		assertNull(notificationDao.load(notification.getId()));
	}

}