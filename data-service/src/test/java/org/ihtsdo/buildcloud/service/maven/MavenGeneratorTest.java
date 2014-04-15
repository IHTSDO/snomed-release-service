package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.TestMavenArtifact;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
public class MavenGeneratorTest {
	
	@Autowired
	private MavenGenerator mavenGenerator;

	private TestEntityFactory testEntityFactory;

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	@Before
	public void setup() {
		testEntityFactory = new TestEntityFactory();
	}

	@Test
	public void testGenerateBuildScripts() throws IOException {
		Execution execution = testEntityFactory.createExecution();
		String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);
		String expectedRootPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-generated-build-pom.txt"), Charset.defaultCharset()).replace("\r", "");
		String expectedModulePom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-generated-build-module-pom.txt"), Charset.defaultCharset()).replace("\r", "");

		File buildDirectory = mavenGenerator.generateBuildScripts(jsonConfig);

		Assert.assertNotNull(buildDirectory);

		String generatedRootPom = fileToString(new File(buildDirectory, "pom.xml"));
		Assert.assertEquals(expectedRootPom, generatedRootPom);
		String generatedModulePom = fileToString(new File(new File(buildDirectory, "rf2_release"), "pom.xml"));
		Assert.assertEquals(expectedModulePom, generatedModulePom);
		Assert.assertEquals("Expecting 10 things. (4 x pom.xml, 3 x assembly.xml, 3 x dir)", TestUtils.itemCount(buildDirectory), 10);

	}

	@Test
	public void testGenerateArtifactPom() throws IOException {
		String expectedPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-artifact-pom.txt"), Charset.defaultCharset()).replace("\r", "");
		StringWriter writer = new StringWriter();

		mavenGenerator.generateArtifactPom(new TestMavenArtifact("org.ihtsdo.release.international.international.spanish_edition.biannual", "input.rf2", "1.0", "zip"), writer);

		String actualPom = writer.toString();
		Assert.assertEquals(expectedPom, actualPom);
	}
	
	@Test
	public void testGenerateManifestArtifactPom() throws IOException {
		String expectedPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-manifest-artifact-pom.txt"), Charset.defaultCharset()).replace("\r", "");
		StringWriter writer = new StringWriter();

		Map <String, String> metaData = new HashMap<String, String>();
		metaData.put("isManifest", "true");
		mavenGenerator.generateArtifactPom(new TestMavenArtifact("org.ihtsdo.release.international.international.spanish_edition.biannual", "input.rf2", "1.0", "zip", metaData), writer);

		String actualPom = writer.toString();
		Assert.assertEquals(expectedPom, actualPom);
	}

	@Test
	public void testGetArtifact() throws IOException {
		Package aPackage = testEntityFactory.createPackage("the center", "center", "ex", "prod", "build1", "myPackage");
		InputFile in1 = new InputFile("in1", "1.0");
		aPackage.addInputFile(in1);
		Assert.assertEquals("center", in1.getPackage().getBuild().getProduct().getExtension().getReleaseCenter().getBusinessKey());

		mavenGenerator.generateArtifactAndGroupId(in1);

		Assert.assertEquals("org.ihtsdo.release.center.ex.prod.build1", in1.getGroupId());
		Assert.assertEquals("mypackage.input.in1", in1.getArtifactId());
		Assert.assertEquals("1.0", in1.getVersion());
	}

	private String fileToString(File file) throws IOException {
		return StreamUtils.copyToString(new FileInputStream(file), Charset.defaultCharset()).replace("\r", "");
	}

}
