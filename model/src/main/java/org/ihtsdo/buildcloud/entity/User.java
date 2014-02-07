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

	private String oauthId;

	@OneToMany(mappedBy = "user")
	private List<ReleaseCentreMembership> releaseCentreMemberships;

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

	public List<ReleaseCentreMembership> getReleaseCentreMemberships() {
		return releaseCentreMemberships;
	}

	public void setReleaseCentreMemberships(List<ReleaseCentreMembership> releaseCentreMemberships) {
		this.releaseCentreMemberships = releaseCentreMemberships;
	}

}
