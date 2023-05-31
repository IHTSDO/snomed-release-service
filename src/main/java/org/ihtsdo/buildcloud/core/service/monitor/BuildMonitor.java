package org.ihtsdo.buildcloud.core.service.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Notification;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BuildMonitor extends Monitor {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private final Product product;
	private final String buildKey;
	private final BuildDAO buildDAO;
	private final ObjectMapper objectMapper;
	private long maxTimeToRunInMiliSecond;
	private final long maxTimeToRunInMinute;
	private final String recipient;

	public BuildMonitor(Product product, String buildKey, String recipient, int maxTimeToRunInMinute, BuildDAO buildDAO, ObjectMapper objectMapper) {
		this.product = product;
		this.buildKey = buildKey;
		this.buildDAO = buildDAO;
		this.objectMapper = objectMapper;
		this.maxTimeToRunInMinute = maxTimeToRunInMinute;
		this.maxTimeToRunInMiliSecond = maxTimeToRunInMinute * 60 * 1000;
		this.recipient = recipient;
	}

	@Override
	public Notification runOnce() throws MonitorException {
		try {
			Build build = buildDAO.find(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), this.buildKey, false, false, false, null);
			if (build != null) {
				if (!isBuildRunning(build)) {
					throw new MonitorException("The build is not not running so it will not be monitored anymore.");
				}
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

				Date buildCreatedDate = simpleDateFormat.parse(build.getId());
				long currentTime = new Date().getTime();
				if ((currentTime - buildCreatedDate.getTime() > maxTimeToRunInMiliSecond)) {
					Notification notification = new Notification();
					notification.setRead(false);
					notification.setRecipient(recipient);
					notification.setNotificationType(Notification.NotificationType.BUILD_RUN_OUT_OF_TIME.name());

					Map<String, String> details = new HashMap<>();
					details.put("releaseCenterKey", this.product.getReleaseCenter().getBusinessKey());
					details.put("productKey", this.product.getBusinessKey());
					details.put("buildKey", this.buildKey);

					String message = String.format("The build %s in %s product in %s took more than %s minutes to execute.", build.getId(), product.getName(), product.getReleaseCenter().getName(), this.maxTimeToRunInMinute);
					details.put("message", message);

					notification.setDetails(objectMapper.writeValueAsString(details));

					return notification;
				}
			}
			return null;
		} catch (Exception e) {
			throw new MonitorException("Failed to get build state", e);
		}
	}

	private boolean isBuildRunning(Build build) {
		Build.Status status = build.getStatus();
		return Build.Status.QUEUED == status
				|| Build.Status.PENDING == status
				|| Build.Status.BEFORE_TRIGGER == status
				|| Build.Status.BUILDING == status
				|| Build.Status.RVF_QUEUED == status
				|| Build.Status.RVF_RUNNING == status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BuildMonitor that = (BuildMonitor) o;

		if (!Objects.equals(this.product.getReleaseCenter().getBusinessKey(), that.product.getReleaseCenter().getBusinessKey())) return false;
		if (!Objects.equals(this.product.getBusinessKey(), that.product.getBusinessKey())) return false;
		return Objects.equals(this.buildKey, that.buildKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), buildKey);
	}

	@Override
	public String toString() {
		return "BuildMonitor{" +
				"releaseCenterKey='" + product.getReleaseCenter().getBusinessKey() + '\'' +
				", productKey='" + product.getBusinessKey() + '\'' +
				", buildKey='" + buildKey + '\'' +
				", recipient='" + recipient + '\'' +
				'}';
	}
}
