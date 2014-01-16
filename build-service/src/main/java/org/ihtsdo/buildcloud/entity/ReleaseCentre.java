package org.ihtsdo.buildcloud.entity;

import javax.persistence.*;

@Entity
public class ReleaseCentre {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Column(unique = true)
	private String webName;

	public ReleaseCentre() {
	}

	public ReleaseCentre(String name) {
		setName(name);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		if (name != null) {
			webName = name.toLowerCase().replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
		} else {
			webName = null;
		}
	}

	public String getWebName() {
		return webName;
	}

}
