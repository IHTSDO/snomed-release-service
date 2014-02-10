package org.ihtsdo.buildcloud.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
public class User {

	@Id
	@GeneratedValue
	private Long id;

	private String oauthId;

	@OneToMany(mappedBy = "user")
	private Set<ReleaseCentreMembership> releaseCentreMemberships;

	public User() {
	}

	public User(String oauthId) {
		this();
		this.oauthId = oauthId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOauthId() {
		return oauthId;
	}

	public void setOauthId(String oauthId) {
		this.oauthId = oauthId;
	}

	public Set<ReleaseCentreMembership> getReleaseCentreMemberships() {
		return releaseCentreMemberships;
	}

	public void setReleaseCentreMemberships(Set<ReleaseCentreMembership> releaseCentreMemberships) {
		this.releaseCentreMemberships = releaseCentreMemberships;
	}

}
