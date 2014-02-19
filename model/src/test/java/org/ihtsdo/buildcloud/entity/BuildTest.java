package org.ihtsdo.buildcloud.entity;

import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;


public class BuildTest extends TestEntityGenerator{

	@Test
	public void testStructure() {

		TestEntityFactory f = new TestEntityFactory();
		Build build = f.createBuild();
		Assert.assertNotNull(build);
		Assert.assertEquals(build.getPackages().size(), packageNames.length);
	}

	@Test
	public void testConfig() {

		TestEntityFactory f = new TestEntityFactory();
		Build build = f.createBuild();
		Map<String, Object> configMap = build.getConfig();
		Assert.assertNotNull(configMap);

		ArrayList<Map<String, Object>> packageConfig = (ArrayList<Map<String, Object>>) configMap.get("Packages");
		Assert.assertNotNull(packageConfig);
		Assert.assertEquals(packageConfig.size(), packageNames.length);
	}

}
