package org.ihtsdo.buildcloud.service.precondition;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class PreconditionManagerTest {
	
	private static final String PPP = "SomeValue.zip";

	@Autowired
	protected BuildDAO buildDAO;
	
	protected Build build;
	protected Execution execution;
	protected PreconditionManager mgr;
	
	@Before
	public void setup() {
		build = buildDAO.find(1L, TestEntityGenerator.TEST_USER);
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 01).getTime();
		execution = new Execution(creationTime, build);
		mgr = new PreconditionManager(execution);
		mgr.add(new CheckFirstReleaseFlag());
	}

	@Test
	public void testFirstCorrect() {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(true);
			p.setPreviousPublishedPackage(null);
		}
		
		String actualResult = runPreConditionChecks();
		Assert.assertEquals( PreconditionCheck.State.PASS.toString(), actualResult);		

	}
	
	@Test
	public void testFirstIncorrect() {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(true);
			p.setPreviousPublishedPackage(PPP);
		}
		
		String actualResult = runPreConditionChecks();
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);		
	}
	
	@Test
	public void testSubsequentCorrect() {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(false);
			p.setPreviousPublishedPackage(PPP);
		}
		
		String actualResult = runPreConditionChecks();
		Assert.assertEquals( PreconditionCheck.State.PASS.toString(), actualResult);	
	}
	
	@Test
	public void testSubsequentIncorrect() {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(false);
			p.setPreviousPublishedPackage(null);
		}
		
		String actualResult = runPreConditionChecks();
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);	
	}
	
	private String runPreConditionChecks() {
		Map<String, Object> report = mgr.runPreconditionChecks();
		Assert.assertNotNull(report);
		
		@SuppressWarnings("unchecked") //Am hiding complexity from external code components, but keeping strict type checking within the class
		List<Map<PreconditionCheck.ResponseKey,String>> allPrechecks = List.class.cast(report.get(TestEntityGenerator.packageNames[0][0][0]));		//For the "Snomed Release Package"
		Map<PreconditionCheck.ResponseKey,String> testResults = allPrechecks.get(0);												//Get the first test run
		
		String testName = testResults.get(PreconditionCheck.ResponseKey.PRE_CHECK_NAME);
		Assert.assertEquals (CheckFirstReleaseFlag.class.getSimpleName(), testName);
		
		return testResults.get(PreconditionCheck.ResponseKey.RESULT);
	}

}
