package org.ihtsdo.buildcloud.service.maven;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenArtifactTest {

	private MavenArtifact artifact;

	@Before
	public void setup() {
		artifact = new MavenArtifact("some.groupId", "the.artifactId", "version-1.0", "zip");
	}

	@Test
	public void testGetPath() {
		String artifactPath = artifact.getPath();
		Assert.assertEquals("some/groupId/the/artifactId/version-1.0/the.artifactId-version-1.0.zip", artifactPath);
	}

	@Test
	public void testGetPomPath() {
		String pomPath = artifact.getPomPath();
		Assert.assertEquals("some/groupId/the/artifactId/version-1.0/the.artifactId-version-1.0.pom", pomPath);
	}

}
