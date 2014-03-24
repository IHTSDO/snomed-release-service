package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.MavenArtifact;
import org.ihtsdo.buildcloud.entity.TestMavenArtifact;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenArtifactHelperTest {

	private MavenArtifactHelper helper;
	private MavenArtifact artifact;

	@Before
	public void setup() {
		helper = new MavenArtifactHelper();
		artifact = new TestMavenArtifact("some.groupId", "the.artifactId", "version-1.0", "zip");
	}

	@Test
	public void testGetPath() {
		String artifactPath = helper.getPath(artifact);
		Assert.assertEquals("some/groupId/the.artifactId/version-1.0/the.artifactId-version-1.0.zip", artifactPath);
	}

	@Test
	public void testGetPomPath() {
		String pomPath = helper.getPath(artifact, MavenArtifact.POM);
		Assert.assertEquals("some/groupId/the.artifactId/version-1.0/the.artifactId-version-1.0.pom", pomPath);
	}

}
