package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.MavenArtifactHelper;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity

public class InputFile implements MavenArtifact, DomainEntity {

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
	}

	public InputFile(String name) {
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
	public DomainEntity getParent() {
		return packag;
	}

	@Override
	public String getCollectionName() {
		return "inputFiles";
	}
}
