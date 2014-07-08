package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public abstract class PreconditionCheckTest {

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ExecutionDAO executionDAO;

	@Autowired
	private TestUtils testUtils;

	protected Build build;
	protected Execution execution = null;
	
	protected PreconditionManager manager;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionCheckTest.class);
	
	static int executionIdx = 0;
	
	@Before
	public void setup() {
		build = buildDAO.find(1L, TestEntityGenerator.TEST_USER);
	}
	
	protected void createNewExecution() {
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, executionIdx++).getTime();
		execution = new Execution(creationTime, build);
		
		//Because we're working with a unit test, that execution will probably already exist on disk, so wipe
		testUtils.scrubExecution(execution);
		
		// Copy all files from Build input and manifest directory to Execution input and manifest directory
		executionDAO.copyAll(build, execution);
	}

	protected String runPreConditionCheck(Class<? extends PreconditionCheck> classUnderTest) throws InstantiationException, IllegalAccessException {
		
		//Do we need an execution?
		if (execution == null) {
			createNewExecution();
		}
		
		//Create a manager for this test
		Map<String, Object> report = manager.runPreconditionChecks(execution);
		Assert.assertNotNull(report);

		@SuppressWarnings("unchecked") //Am hiding complexity from external code components, but keeping strict type checking within the class
		List<Map<PreconditionCheck.ResponseKey,String>> allPrechecks = List.class.cast(report.get(TestEntityGenerator.packageNames[0][0][0]));		//For the "Snomed Release Package"
		Map<PreconditionCheck.ResponseKey,String> testResults = allPrechecks.get(0);												//Get the first test run
		
		String testName = testResults.get(PreconditionCheck.ResponseKey.PRE_CHECK_NAME);
		Assert.assertEquals (classUnderTest.getSimpleName(), testName);
		
		String resultString = testResults.get(PreconditionCheck.ResponseKey.RESULT);
		
		//If it's a fail, we'll debug that message just for testing purposes
		if (!resultString.equals(PreconditionCheck.State.PASS))
			LOGGER.warn ("Test {} Reported {}",testName,testResults.get(PreconditionCheck.ResponseKey.MESSAGE));
		return resultString;
	}
	
}
