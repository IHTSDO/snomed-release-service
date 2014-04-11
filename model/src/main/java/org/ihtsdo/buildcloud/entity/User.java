package org.ihtsdo.buildcloud.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class User {

	@Id
	@GeneratedValue
	private Long id;

	private String username;

	private String encodedPassword;

	@OneToMany(mappedBy = "user")
	private List<ReleaseCenterMembership> releaseCenterMemberships;

	public User() {
	}

	public User(String username) {
		this();
		this.username = username;
	}

	public User(Long id, String username) {
		this(username);
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEncodedPassword() {
		return encodedPassword;
	}

	public void setEncodedPassword(String encodedPassword) {
		this.encodedPassword = encodedPassword;
	}

	public List<ReleaseCenterMembership> getReleaseCenterMemberships() {
		return releaseCenterMemberships;
	}

	public void setReleaseCenterMemberships(List<ReleaseCenterMembership> releaseCenterMemberships) {
		this.releaseCenterMemberships = releaseCenterMemberships;
	}

}
