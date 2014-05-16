package org.ihtsdo.buildcloud.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class AuthToken {

	@Id
	private String token;

	@ManyToOne
	private User user;

	public AuthToken() {
	}

	public AuthToken(String token, User user) {
		this.token = token;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public User getUser() {
		return user;
	}

}
