package org.ihtsdo.buildcloud.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="auth_token")
public class AuthToken {

	@Id
	private String token;

	@ManyToOne
	private User user;

	public AuthToken() {
	}

	public AuthToken(final String token, final User user) {
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
