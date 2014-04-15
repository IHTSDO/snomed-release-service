package org.ihtsdo.buildcloud.entity;

import java.util.Map;

public class TestMavenArtifact implements MavenArtifact {

	private String groupId;
	private String artifactId;
	private String version;
	private String packaging;
	private Map<String, String> metaData;

	public TestMavenArtifact(String groupId, String artifactId, String version, String packaging) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
	}

	public TestMavenArtifact(String groupId, String artifactId, String version, String packaging, Map<String, String> metaData) {
		this(groupId,artifactId,version,packaging);
		this.setMetaData(metaData);
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	public Map<String, String> getMetaData() {
		return metaData;
	}

	public void setMetaData(Map<String, String> metaData) {
		this.metaData = metaData;
	}

}
