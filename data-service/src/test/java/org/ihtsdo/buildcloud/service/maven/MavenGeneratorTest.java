package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class MavenGeneratorTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	private MavenGenerator mavenGenerator;
	private TestEntityFactory testEntityFactory;

	@Before
	public void setup() {
		mavenGenerator = new MavenGenerator();
		testEntityFactory = new TestEntityFactory();
	}

	@Test
	public void testGenerateBuildPoms() throws IOException {
		Build build = testEntityFactory.createBuild();
		String expectedPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-generated-build-pom.txt"), Charset.defaultCharset()).replace("\r", "");

		File buildDirectory = mavenGenerator.generateBuildFiles(build);
		Assert.assertNotNull(buildDirectory);
		
		String generatedPom = StreamUtils.copyToString(new FileInputStream(new File(buildDirectory, "pom.xml")), Charset.defaultCharset()).replace("\r", "");
		Assert.assertEquals(expectedPom, generatedPom);
		
		//We're expecting to have 10 things in that directory
		//4 x pom.xml, 3 x assembly.xml, 3 x dir
		int directoryItemCount = TestUtils.itemCount(buildDirectory);
		LOGGER.debug ("Found " + directoryItemCount + " items in " + buildDirectory.getName());
		Assert.assertEquals(directoryItemCount, 10);	
	}

	@Test
	public void testGenerateArtifactPom() throws IOException {
		String expectedPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-artifact-pom.txt"), Charset.defaultCharset()).replace("\r", "");

		StringWriter writer = new StringWriter();
		mavenGenerator.generateArtifactPom(writer, new MavenArtifact("org.ihtsdo.release.international.international.spanish_edition.biannual", "input.rf2", "1.0", "zip"));
		String actualPom = writer.toString();

		Assert.assertEquals(expectedPom, actualPom);
	}

	@Test
	public void testGetPomPath() {
		String pomPath = mavenGenerator.getPath(new MavenArtifact("some.groupId", "the.artifactId", "version-1.0"));
		Assert.assertEquals("some/groupId/the/artifactId/version-1.0/the.artifactId-version-1.0.pom", pomPath);
	}

	@Test
	public void testGetArtifactPath() {
		String artifactPath = mavenGenerator.getPath(new MavenArtifact("some.groupId", "the.artifactId", "version-1.0", "zip"));
		Assert.assertEquals("some/groupId/the/artifactId/version-1.0/the.artifactId-version-1.0.zip", artifactPath);
	}

	@Test
	public void testGetArtifact() throws IOException {
		Package aPackage = testEntityFactory.createPackage("the centre", "centre", "ex", "prod", "build1", "myPackage");
		InputFile in1 = new InputFile("in1");
		aPackage.addInputFile(in1);

		MavenArtifact artifact = mavenGenerator.getArtifact(in1);

		Assert.assertEquals("org.ihtsdo.release.centre.ex.prod.build1", artifact.getGroupId());
		Assert.assertEquals("mypackage.input.in1", artifact.getArtifactId());
		Assert.assertEquals("1.0", artifact.getVersion());
	}

}
