package org.ihtsdo.buildcloud.service.mapping;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
public class ExecutionConfigurationJsonGeneratorTest {

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	private org.ihtsdo.buildcloud.entity.Package internationalPackage;
	private String expectedExport;
	private Execution execution;

	@Before
	public void setup() throws IOException {
		TestEntityFactory factory = new TestEntityFactory();
		internationalPackage = factory.createPackage(
				"International Release Centre", "International", "SNOMED CT International Edition",
				"SNOMED CT International Edition", "International Release", "RF2 Release");
		execution = new Execution(new GregorianCalendar(2013, 2, 5, 16, 30, 00).getTime(), internationalPackage.getBuild());
		expectedExport = FileCopyUtils.copyToString(new InputStreamReader(this.getClass().getResourceAsStream("expected-build-config-export.json")));
	}

	@Test
	public void testGetConfig() throws IOException, JSONException {
		List<InputFile> inputFiles = internationalPackage.getInputFiles();
		Assert.assertEquals(1, inputFiles.size());
		inputFiles.get(0).setVersionDate(new GregorianCalendar(2014, 2, 18, 15, 30, 00).getTime());

		String actual = executionConfigurationJsonGenerator.getJsonConfig(execution);

		JSONAssert.assertEquals(expectedExport, actual, false);
	}

}
