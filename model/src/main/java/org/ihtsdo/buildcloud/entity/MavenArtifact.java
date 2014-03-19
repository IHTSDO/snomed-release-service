package org.ihtsdo.buildcloud.entity;

public interface MavenArtifact {

	String POM = "pom";

	String getGroupId();
	String getArtifactId();
	String getVersion();
	String getPackaging();

}
