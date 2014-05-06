package org.ihtsdo.buildcloud.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class AuthToken {

	@Id
	private String token;

	private Date expires;

	@ManyToOne
	private User user;

	public AuthToken() {
	}

	public AuthToken(String token, Date expires, User user) {
		this.token = token;
		this.expires = expires;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public Date getExpires() {
		return expires;
	}

	public User getUser() {
		return user;
	}

}
