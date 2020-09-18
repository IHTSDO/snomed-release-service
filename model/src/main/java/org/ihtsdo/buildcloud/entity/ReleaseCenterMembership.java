package org.ihtsdo.buildcloud.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@JsonPropertyOrder({"id", "name"})
@Table(name="membership")
public class ReleaseCenterMembership {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	@JoinColumn(name="release_center_id")
	private ReleaseCenter releaseCenter;

	@ManyToOne
	@JoinColumn(name="user_id")
	private User user;

	private Role role;

	public ReleaseCenterMembership() {
	}

	public ReleaseCenterMembership(final ReleaseCenter releaseCenter, final User user) {
		this.releaseCenter = releaseCenter;
		this.user = user;
		this.role = Role.VIEW;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(final ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public User getUser() {
		return user;
	}

	public void setUser(final User user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(final Role role) {
		this.role = role;
	}

}
