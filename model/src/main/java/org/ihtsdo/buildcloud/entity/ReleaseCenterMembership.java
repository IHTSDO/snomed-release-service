package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@JsonPropertyOrder({"id", "name"})
public class ReleaseCenterMembership {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private ReleaseCenter releaseCenter;

	@ManyToOne
	private User user;

	private Role role;

	public ReleaseCenterMembership() {
	}

	public ReleaseCenterMembership(ReleaseCenter releaseCenter, User user) {
		this.releaseCenter = releaseCenter;
		this.user = user;
		this.role = Role.VIEW;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

}
