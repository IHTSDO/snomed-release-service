package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
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

import java.io.*;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test/testDataServiceContext.xml"})
@Transactional
public abstract class PreconditionCheckTest {

	@Autowired
	protected ProductInputFileService productInputFileService;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ExecutionDAO executionDAO;

	@Autowired
	private ProductInputFileDAO productInputFileDAO;

	@Autowired
	private TestUtils testUtils;

	protected Product product;
	protected Execution execution = null;
	protected PreconditionManager manager;

	protected static final String JULY_RELEASE = "20140731";

	private static int executionIdx = 0;
	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionCheckTest.class);

	@Before
	public void setup() throws Exception {
		product = productDAO.find(1L, TestUtils.TEST_USER);
		if (product.getEffectiveTime() == null) {
			product.setEffectiveTime(RF2Constants.DATE_FORMAT.parse(JULY_RELEASE));
		}
	}

	protected void createNewExecution() {
		Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, executionIdx++).getTime();
		execution = new Execution(creationTime, product);

		// Because we're working with a unit test, that execution will probably already exist on disk, so wipe
		testUtils.scrubExecution(execution);

		// Copy all files from Product input and manifest directory to Execution input and manifest directory
		executionDAO.copyAll(product, execution);
	}

	protected PreConditionCheckReport runPreConditionCheck(Class<? extends PreconditionCheck> classUnderTest) throws InstantiationException, IllegalAccessException {

		// Do we need an execution? // TODO: remove this - we should always know the state of a test
		if (execution == null) {
			createNewExecution();
		}

		// Create a manager for this test
		List<PreConditionCheckReport> report = manager.runPreconditionChecks(execution);
		Assert.assertNotNull(report);

		PreConditionCheckReport testResult = report.get(0); // Get the first test run

		String testName = testResult.getPreConditionCheckName();
		Assert.assertEquals(classUnderTest.getSimpleName(), testName);

		// If it's a fail, we'll debug that message just for testing purposes
		if (State.PASS != testResult.getResult()) {
			LOGGER.warn("Test {} Reported {}", testName, testResult.getMessage());
		}
		return testResult;
	}

	protected void loadManifest(String filename) throws FileNotFoundException {
		if (filename != null) {
			String testFilePath = getClass().getResource(filename).getFile();
			File testManifest = new File(testFilePath);
			productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
		} else {
			productInputFileDAO.deleteManifest(product);
		}

		//When we load a manifest, we need that copied over to a new execution
		createNewExecution();
	}

	/**
	 * call before loadManifest.
	 *
	 * @param filename
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	protected void addEmptyFileToInputDirectory(String filename) throws ResourceNotFoundException, IOException {
		File tempFile = File.createTempFile("testTemp", ".txt");
		try (InputStream inputStream = new FileInputStream(tempFile)) {
			productInputFileService.putInputFile(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), inputStream, filename, 0L);
		} finally {
			tempFile.deleteOnExit();
		}
	}

	protected void deleteFilesFromInputFileByPattern(String fileExtension) throws ResourceNotFoundException {
		productInputFileService.deleteFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), fileExtension);
	}

}
