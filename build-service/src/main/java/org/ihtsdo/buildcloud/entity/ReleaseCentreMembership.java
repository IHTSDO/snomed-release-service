package org.ihtsdo.buildcloud.entity;

import javax.persistence.*;
import java.util.Set;

@Entity
public class ReleaseCentreMembership {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private ReleaseCentre releaseCentre;

	@ManyToOne
	private User user;

	private Role role;

	public ReleaseCentreMembership() {
	}

	public ReleaseCentreMembership(ReleaseCentre releaseCentre, User user) {
		this.releaseCentre = releaseCentre;
		this.user = user;
		this.role = Role.VIEW;
	}

	public ReleaseCentre getReleaseCentre() {
		return releaseCentre;
	}

	public void setReleaseCentre(ReleaseCentre releaseCentre) {
		this.releaseCentre = releaseCentre;
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
