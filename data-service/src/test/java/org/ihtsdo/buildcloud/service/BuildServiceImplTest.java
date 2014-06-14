package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class BuildServiceImplTest extends TestEntityGenerator {
	
	@Autowired
	private BuildService bs;
	
	@Test
	public void testFindForExtension() {
		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		List<Build> builds = bs.findForExtension(EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]),
												 EntityHelper.formatAsBusinessKey(extensionNames[0]), 
												 filterOff, 
												 TestEntityGenerator.TEST_USER);
		Assert.assertEquals(TestEntityGenerator.buildCount[0], builds.size());
	}
	
	@Test
	public void testFindForExtension_Filtered() {
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.STARRED_ONLY);
		List<Build> builds = bs.findForExtension(EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]),
												 EntityHelper.formatAsBusinessKey(extensionNames[0]), 
												 filterOn, 
												 TestEntityGenerator.TEST_USER);
		Assert.assertEquals(TestEntityGenerator.starredCount[0], builds.size());
	}

	@Test
	public void testCreate() throws Exception{

		Assert.assertNotNull(bs);
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		List<Build> builds = bs.findAll(filterOptions, TestEntityGenerator.TEST_USER);
		int before = builds.size();
		//LOGGER.warn("Found " + before + " builds");
		Assert.assertTrue(before > 0);  //Check our test data is in there.
		bs.create(EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]),
				  EntityHelper.formatAsBusinessKey(extensionNames[0]), 
				  EntityHelper.formatAsBusinessKey(productNames[0][0]), 
				  "my test build name", 
				  TestEntityGenerator.TEST_USER);
		int after = bs.findAll(filterOptions, TestEntityGenerator.TEST_USER).size();
		Assert.assertEquals(before + 1, after);
		
		//TODO Could add further tests to ensure that the new item was created at the correct point in the hierarchy
	}
	
	@Test
	public void testStarredFilter() throws Exception{

		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.STARRED_ONLY);
		List<Build> builds = bs.findAll(filterOff, TestEntityGenerator.TEST_USER);
		int allBuildCount = builds.size();
		
		Assert.assertEquals(TestEntityGenerator.totalBuildCount, allBuildCount);  //Check our test data is in there.
		int starredBuildCount = bs.findAll(filterOn, TestEntityGenerator.TEST_USER).size();
		Assert.assertEquals(TestEntityGenerator.totalStarredBuilds, starredBuildCount);
	}	
	
	@Test
	public void testRemovedFilter() throws Exception{

		EnumSet<FilterOption> filterOff = EnumSet.noneOf(FilterOption.class);
		EnumSet<FilterOption> filterOn = EnumSet.of(FilterOption.INCLUDE_REMOVED);
		List<Build> builds = bs.findAll(filterOff, TestEntityGenerator.TEST_USER);
		int visibleBuildCount = builds.size();
		Assert.assertTrue(visibleBuildCount > 0);
		
		int includeRemovedCount = bs.findAll(filterOn, TestEntityGenerator.TEST_USER).size();
		Assert.assertTrue(includeRemovedCount > 0);
		
		//TODO When remove functionality is built, use it to remove a build and check
		//that our build count goes up if we inclue removed builds.
	}		

}
