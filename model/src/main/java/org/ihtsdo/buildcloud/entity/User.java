package org.ihtsdo.buildcloud.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;


@Entity
@Table(name="user")
public class User {

	public static final String ANONYMOUS_USER = "anonymous_user";

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;
	@Column(name="user_name")
	private String username;

	@JsonIgnore
	@Column(name="encoded_password")
	private String encodedPassword;

	@OneToMany(mappedBy = "user")
	@JsonIgnore
	private List<ReleaseCenterMembership> releaseCenterMemberships;

	public User() {
	}

	public User(final String username) {
		this();
		this.username = username;
	}

	public User(final Long id, final String username) {
		this(username);
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getEncodedPassword() {
		return encodedPassword;
	}

	public void setEncodedPassword(final String encodedPassword) {
		this.encodedPassword = encodedPassword;
	}

	public List<ReleaseCenterMembership> getReleaseCenterMemberships() {
		return releaseCenterMemberships;
	}

	public void setReleaseCenterMemberships(final List<ReleaseCenterMembership> releaseCenterMemberships) {
		this.releaseCenterMemberships = releaseCenterMemberships;
	}

}
