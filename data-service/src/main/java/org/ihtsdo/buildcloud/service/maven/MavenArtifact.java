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

	public String getPath() {
		return getPath(getPackaging());
	}

	public String getPomPath() {
		return getPath("pom");
	}

	private String getPath(String packaging) {
		String groupIdWithSlashes = withSlashes(groupId);
		String artifactIdWithSlashes = withSlashes(artifactId);
		return String.format("%s/%s/%s/%s-%3$s.%s", groupIdWithSlashes, artifactIdWithSlashes, version, artifactId, packaging);
	}

	private String withSlashes(String name) {
		return name.replace(".", "/");
	}

}
