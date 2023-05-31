package org.ihtsdo.buildcloud.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.NotificationDao;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private NotificationDao dao;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public Notification create(Notification notification) {
		dao.save(notification);
		return notification;
	}

	@Override
	public Page<Notification> findAll(String recipient, PageRequest pageRequest) {
		return dao.findAll(recipient, pageRequest);
	}

	@Override
	public Long countUnreadNotifications(String username) {
		return dao.countUnreadNotifications(username);
	}

	@Override
	public List<Long> removeNotifications(List<Long> notificationIds) {
		String currentUser = SecurityUtil.getUsername();
		List<Notification> notifications;
		if (notificationIds == null || notificationIds.isEmpty()) {
			notifications = dao.findByRecipient(currentUser);
		} else {
			notifications = dao.findByIds(notificationIds);
		}
		List<Long> removedNotificationIds = new ArrayList<>();
		for (Notification notification : notifications) {
			removedNotificationIds.add(notification.getId());
			dao.delete(notification);
		}

		try {
			Map<String, Object> message = new HashMap<>();
			message.put("event", "DELETE_NOTIFICATIONS");
			simpMessagingTemplate.convertAndSend("/topic/user/" + currentUser + "/notification", objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			logger.error("Failed to send message through web-socket", e);
		}
		return removedNotificationIds;
	}

	@Override
	public List<Long> markNotificationsAsRead(List<Long> notificationIds) {
		String currentUser = SecurityUtil.getUsername();
		List<Notification> notifications = dao.findByIds(notificationIds);
		List<Long> readNotifications = new ArrayList<>();
		for (Notification notification : notifications) {
			notification.setRead(Boolean.TRUE);
			readNotifications.add(notification.getId());
			dao.update(notification);
		}
		try {
			Map<String, Object> message = new HashMap<>();
			message.put("event", "MARK_NOTIFICATIONS_AS_READ");
			simpMessagingTemplate.convertAndSend("/topic/user/" + currentUser + "/notification", objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			logger.error("Failed to send message through web-socket", e);
		}
		return readNotifications;
	}
}
