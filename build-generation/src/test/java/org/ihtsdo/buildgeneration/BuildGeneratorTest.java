package org.ihtsdo.buildgeneration;

import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class BuildGeneratorTest {

	private BuildGenerator buildGenerator;
	private String expectedPom;
	private Package releasePackage;

	@Before
	public void setup() throws IOException {
		buildGenerator = new BuildGenerator();
		expectedPom = StreamUtils.copyToString(this.getClass().getResourceAsStream("expected-generated-pom.txt"), Charset.defaultCharset()).replace("\r", "");
		releasePackage = new TestEntityFactory().createPackage("International", "International", "Spanish Edition", "Biannual", "RF2");
	}

	@Test
	public void test() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		buildGenerator.generate(new OutputStreamWriter(out), releasePackage.getBuild());
		String actualPom = out.toString();

		Assert.assertEquals(expectedPom, actualPom);
	}

}
