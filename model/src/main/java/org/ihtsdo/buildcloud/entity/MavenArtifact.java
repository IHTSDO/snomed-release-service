package org.ihtsdo.buildcloud.entity;

import java.util.Map;

public interface MavenArtifact {

	String POM = "pom";

	String getGroupId();
	String getArtifactId();
	String getVersion();
	String getPackaging();
	Map<String, String> getMetaData();

}
