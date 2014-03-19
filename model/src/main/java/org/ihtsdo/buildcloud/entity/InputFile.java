package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class InputFile {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	@JsonProperty("id")
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private Package packag;

	private String version;

	public InputFile() {
	}

	public InputFile(String name) {
		setName(name);
	}

	public InputFile(String name, String version) {
		this(name);
		this.version = version;
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
		generateBusinessKey();
	}

	public void setVersionDate(Date versionDate) {
		this.version = EntityHelper.formatAsIsoDateTimeURLCompatible(versionDate);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	@JsonIgnore
	public Package getPackage() {
		return packag;
	}

	public void setPackage(Package packag) {
		this.packag = packag;
	}

	public String getVersion() {
		return version;
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}
}
