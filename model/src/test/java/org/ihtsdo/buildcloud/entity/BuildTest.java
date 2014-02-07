package org.ihtsdo.buildcloud.entity;

import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Test;

public class BuildTest extends TestEntityGenerator{

	@Test
	public void testStructure() {
		
		TestEntityFactory f = new TestEntityFactory();
		Build build = f.createBuild();
		Assert.assertNotNull(build);
		Assert.assertEquals(build.getPackages().size(), packageNames.length);
	}
	
}
