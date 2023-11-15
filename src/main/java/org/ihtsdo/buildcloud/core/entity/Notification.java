package org.ihtsdo.buildcloud.core.entity;

import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import org.hibernate.type.YesNoConverter;

import java.util.Date;

@Entity
@Table(name = "notification")
public class Notification {

	public enum NotificationType {
		BUILD_RUN_OUT_OF_TIME
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "details")
	private String details;

	@Column(name = "recipient")
	private String recipient;

	@Column(name = "notification_type")
	private String notificationType;

	@Column(name = "created_date")
	private Date createdDate;

	@Convert(converter = YesNoConverter.class)
	@Column(name = "is_read")
	private Boolean read;

	public Notification() {
		this.createdDate = new Date();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
