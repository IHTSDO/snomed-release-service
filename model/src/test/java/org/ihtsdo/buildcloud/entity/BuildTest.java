package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;


public class BuildTest extends TestEntityGenerator{

	private Build build;

	@Before
	public void setup() {
		TestEntityFactory factory = new TestEntityFactory();
		build = factory.createBuild();
	}

	@Test
	public void testStructure() {
		Assert.assertNotNull(build);
		Assert.assertEquals(build.getPackages().size(), packageNames[0][0].length);
	}

	@Test
	public void testJacksonDefaultView() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		StringWriter writer = new StringWriter();

		objectMapper.writeValue(writer, build);

		String actual = writer.toString();
		System.out.println(actual);
		Assert.assertFalse("Default jackson view should not contain packages.", actual.contains("\"packages\""));
	}

}
