package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface NotificationService {

	Notification create(Notification notification);

	Page<Notification> findAll(String username, PageRequest pageRequest);

	Long countUnreadNotifications(String username);

	List<Long> removeNotifications(List<Long> notificationIds);

	List<Long> markNotificationsAsRead(List<Long> notificationIds);
}
