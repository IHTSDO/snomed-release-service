package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ihtsdo.buildcloud.helper.EntityHelper;

import javax.persistence.*;

@Entity
public class Extension {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Column(unique = true)
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private ReleaseCentre releaseCentre;

	public Extension() {
	}

	public Extension(String name, ReleaseCentre releaseCentre) {
		setName(name);
		this.releaseCentre = releaseCentre;
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
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public ReleaseCentre getReleaseCentre() {
		return releaseCentre;
	}

	public void setReleaseCentre(ReleaseCentre releaseCentre) {
		this.releaseCentre = releaseCentre;
	}

}
