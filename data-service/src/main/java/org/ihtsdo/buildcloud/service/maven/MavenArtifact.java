package org.ihtsdo.buildcloud.service.maven;

public class MavenArtifact {

	private String groupId;
	private String artifactId;
	private String version;
	private String packaging;

	public MavenArtifact(String groupId, String artifactId, String version, String packaging) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
	}

	public MavenArtifact(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, "pom");
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}
}
