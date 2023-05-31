package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.BuildStatusTracker;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface NotificationDao extends EntityDAO<Notification> {

	Page<Notification> findAll(String recipient, PageRequest pageRequest);

	Long countUnreadNotifications(String recipient);

	List<Notification> findByRecipient(String recipient);

	List<Notification> findByIds(List<Long> notificationIds);
}
