package org.ihtsdo.buildcloud.service.mapping;

import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.json.JSONException;
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
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class ExecutionConfigurationJsonGeneratorTest {

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	private String expectedExport;
	private Execution execution;

	@Before
	public void setup() throws IOException {
		TestEntityFactory factory = new TestEntityFactory();
		org.ihtsdo.buildcloud.entity.Package internationalPackage = factory.createPackage(
				"International Release Center", "International", "SNOMED CT International Edition",
				"SNOMED CT International Edition", "International Release", "RF2 Release");
		List<String> inputFiles = new ArrayList<>();
		inputFiles.add("sct2_Concept_Delta_INT_20140131.txt");
		inputFiles.add("sct2_Description_Delta-en_INT_20140131.txt");
		inputFiles.add("sct2_Relationship_Delta_INT_20140131.txt");
		internationalPackage.setInputFiles(inputFiles);
		execution = new Execution(new GregorianCalendar(2013, 2, 5, 16, 30, 0).getTime(), internationalPackage.getBuild());
		expectedExport = FileCopyUtils.copyToString(new InputStreamReader(this.getClass().getResourceAsStream("expected-build-config-export.json")));
	}

	@Test
	public void testGetConfig() throws IOException, JSONException {
		String actual = executionConfigurationJsonGenerator.getJsonConfig(execution);
		JSONAssert.assertEquals(expectedExport, actual, false);
	}

}
