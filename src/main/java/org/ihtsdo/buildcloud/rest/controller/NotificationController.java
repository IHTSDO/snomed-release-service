package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.ihtsdo.buildcloud.core.service.NotificationService;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@Tag(name = "Notification", description = "-")
@RequestMapping(value = "/notifications", produces = "application/json")
public class NotificationController {

	@Autowired
	private NotificationService notificationService;

	@GetMapping
	@Operation(summary = "Get notifications for the current user", description = "-")
	@ResponseBody
	public Page<Notification> getNotificationsForCurrentUser(@RequestParam(defaultValue = "0") Integer pageNumber,
                                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                                    HttpServletRequest request) {
        final String username = SecurityUtil.getUsername();
        PageRequest pageRequest = PageRequestHelper.createPageRequest(pageNumber, pageSize, null, null);
        Page<Notification> page = notificationService.findAll(username, pageRequest);
        return new PageImpl<>(page.getContent(), pageRequest, page.getTotalElements());
	}

	@GetMapping("/un-read/count")
	@Operation(summary = "Count number of un-read notifications of current users", description = "-")
	@ResponseBody
	public Map<String, Long> countUnreadNotifications(HttpServletRequest request) {
		final String username = SecurityUtil.getUsername();
		Long result = notificationService.countUnreadNotifications(username);
		Map<String, Long> response = new HashMap<>();
		response.put("total", result);

		return response;
	}

	@DeleteMapping
	@Operation(summary = "Delete a list of notifications")
	public ResponseEntity<Map<String, List<Long>>> deleteNotifications(@RequestParam(required = false) List<Long> notificationIds) {
		Map<String, List<Long>> response = new HashMap<>();
		List<Long> result = notificationService.removeNotifications(notificationIds);
		response.put("removedNotifications", result);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PutMapping("/bulk-mark-as-read")
	@Operation(summary = "Mark a list of notifications as 'read'")
	public ResponseEntity<Map<String, List<Long>>> markNotificationAsRead(@RequestParam List<Long> notificationIds) {
		Map<String, List<Long>> response = new HashMap<>();
		List<Long> result = notificationService.markNotificationsAsRead(notificationIds);
		response.put("readNotifications", result);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
