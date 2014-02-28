package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Package;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
public class BuildServiceImplTest {

	@Autowired
	private BuildService buildService;

	private Package internationalPackage;
	private String expectedExport;

	@Before
	public void setup() throws IOException {
		internationalPackage = new TestEntityFactory().createPackage(
				"International Release Centre", "International", "SNOMED CT International Edition",
				"SNOMED CT International Edition", "International Release", "RF2 Release");
		expectedExport = FileCopyUtils.copyToString(new InputStreamReader(this.getClass().getResourceAsStream("expected-build-config-export.json")));
	}

	@Test
	public void testGetConfig() throws IOException, JSONException {
		Assert.assertEquals(1, internationalPackage.getInputFiles().size());
		Build build = internationalPackage.getBuild();

		String actual = buildService.getConfigJson(build);
		System.out.println(actual);
		JSONAssert.assertEquals(expectedExport, actual, false);
	}

}
