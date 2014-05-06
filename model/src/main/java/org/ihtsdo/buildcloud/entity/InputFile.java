package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.MavenArtifactHelper;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
public class InputFile implements MavenArtifact {

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

	@JsonIgnore
	@ElementCollection(fetch=FetchType.EAGER)
	private Map<String, String> metaData;

	/**
	 * artifactId is generated but stored to guard against renaming.
	 */
	private String artifactId;

	/**
	 * groupId is generated but stored to guard against renaming.
	 */
	private String groupId;

	private String version;

	private static final String packaging = "zip";

	public InputFile() {
		this.metaData = new HashMap<String, String>();
	}

	public InputFile(String name) {
		this();
		setName(name);
	}

	public InputFile(String name, String version) {
		this(name);
		this.version = version;
	}

	public String getPath() {
		return MavenArtifactHelper.getPath(this);
	}

	public String getPomPath() {
		return MavenArtifactHelper.getPath(this, MavenArtifact.POM);
	}

	public void setName(String name) {
		this.name = name;
		generateBusinessKey();
	}

	public void setVersionDate(Date versionDate) {
		this.version = EntityHelper.formatAsIsoDateTimeURLCompatible(versionDate);
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

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	@Override
	public String getPackaging() {
		return packaging;
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	@Override
	@JsonIgnore
	public Map<String, String> getMetaData() {
		return metaData;
	}

	public void setMetaData(Map<String, String> metadata) {
		this.metaData = metadata;
	}

	/**
	 * Sets the metadata for the file if none exists, or appends to existing data if it already exists.
	 */
	public void addMetaData(Map<String, String> metadata) {
		if (this.metaData == null) {
			this.metaData = metadata;
		} else {
			this.metaData.putAll(metadata);
		}
	}

}
