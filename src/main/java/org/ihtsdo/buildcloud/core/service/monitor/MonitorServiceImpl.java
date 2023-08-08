package org.ihtsdo.buildcloud.core.service.monitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.ihtsdo.buildcloud.core.service.NotificationService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Service
public class MonitorServiceImpl implements MonitorService {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final int PAUSE_SECONDS = 120;

	private volatile boolean started;

	private final Set<Monitor> monitors = ConcurrentHashMap.newKeySet();

	@Value("${srs.build.maxTimeToRun}")
	private int maxTimeToRun;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductService productService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void startMonitorBuild(Build build, String username) {
		Monitor monitor = new BuildMonitor(productService.find(build.getReleaseCenterKey(), build.getProductKey(), false), build.getId(), username, maxTimeToRun, buildDAO, objectMapper);
		monitors.add(monitor);
		this.safeStart();
	}

	private void safeStart() {
		if (!started) {
			synchronized (this) {
				if (!started) {
					start();
				}
			}
		}
	}

	private void start() {
		synchronized (this) {
			started = true;
		}
		new Thread(() -> {
			try {
				while (true) {
					Iterator<Monitor> iterator = this.monitors.iterator();
					while (iterator.hasNext()) {
						Monitor monitor = iterator.next();
						if (monitor != null) {
							try {
								final Notification notification = monitor.runOnce();
								if (notification != null) {
									notificationService.create(notification);

									// Remove build from monitoring
									synchronized (monitors) {
										if (monitors.contains(monitor)) {
											iterator.remove();
										}
									}

									try {
										Map<String, Object> message = new HashMap<>();
										message.put("event", "NEW_NOTIFICATION");
										simpMessagingTemplate.convertAndSend("/topic/user/" + notification.getRecipient() + "/notification",  objectMapper.writeValueAsString(message));
									} catch (JsonProcessingException e) {
										logger.error("Failed to send message through web-socket", e);
									}
								}
							} catch (MonitorException e) {
								// Log monitor exception only once per monitor
								synchronized (monitors) {
									if (monitors.contains(monitor)) {
										logger.warn("Monitor run failed, removing {}.", monitor, e);
										iterator.remove();
									}
								}
							}
						}
					}
					Thread.sleep(PAUSE_SECONDS * 1000);
				}
			} catch (InterruptedException e) {
				// This will probably happen when we restart the application.
				logger.info("User monitor interrupted.", e);
			}
		}).start();
	}
}
