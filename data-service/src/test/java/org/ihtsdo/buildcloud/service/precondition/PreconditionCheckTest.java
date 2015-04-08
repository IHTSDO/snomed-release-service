package org.ihtsdo.buildcloud.service.precondition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.ProductInputFileDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test/testDataServiceContext.xml"})
@Transactional
public abstract class PreconditionCheckTest {

	@Autowired
	protected ProductInputFileService productInputFileService;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileDAO productInputFileDAO;

	@Autowired
	private TestUtils testUtils;

	protected Product product;
	protected Build build = null;
	protected PreconditionManager manager;

	protected static final String JULY_RELEASE = "20140731";

	private static int buildIdx = 0;
	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionCheckTest.class);

	@Before
	public void setup() throws Exception {
		product = productDAO.find(1L, TestUtils.TEST_USER);
		if(product.getBuildConfiguration() == null) {
			product.setBuildConfiguration(new BuildConfiguration());
		}
		if (product.getBuildConfiguration().getEffectiveTime() == null) {
			product.getBuildConfiguration().setEffectiveTime(RF2Constants.DATE_FORMAT.parse(JULY_RELEASE));
		}
	}

	protected void createNewBuild() {
		final Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, buildIdx++).getTime();
		build = new Build(creationTime, product);

		// Because we're working with a unit test, that build will probably already exist on disk, so wipe
		testUtils.scrubBuild(build);

		// Copy all files from Product input and manifest directory to Build input and manifest directory
		buildDAO.copyAll(product, build);
	}

	protected PreConditionCheckReport runPreConditionCheck(final Class<? extends PreconditionCheck> classUnderTest) throws InstantiationException, IllegalAccessException {

		// Do we need an build? // TODO: remove this - we should always know the state of a test
		if (build == null) {
			createNewBuild();
		}

		// Create a manager for this test
		final List<PreConditionCheckReport> report = manager.runPreconditionChecks(build);
		Assert.assertNotNull(report);

		final PreConditionCheckReport testResult = report.get(0); // Get the first test run

		final String testName = testResult.getPreConditionCheckName();
		Assert.assertEquals(classUnderTest.getSimpleName(), testName);

		// If it's a fail, we'll debug that message just for testing purposes
		if (State.PASS != testResult.getResult()) {
			LOGGER.warn("Test {} Reported {}", testName, testResult.getMessage());
		}
		return testResult;
	}

	protected void loadManifest(final String filename) throws FileNotFoundException {
		if (filename != null) {
			final String testFilePath = getClass().getResource(filename).getFile();
			final File testManifest = new File(testFilePath);
			productInputFileDAO.putManifestFile(product, new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
		} else {
			productInputFileDAO.deleteManifest(product);
		}

		//When we load a manifest, we need that copied over to a new build
		createNewBuild();
	}

	/**
	 * call before loadManifest.
	 *
	 * @param filename
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	protected void addEmptyFileToInputDirectory(final String filename) throws ResourceNotFoundException, IOException {
		final File tempFile = File.createTempFile("testTemp", ".txt");
		try (InputStream inputStream = new FileInputStream(tempFile)) {
			productInputFileService.putInputFile(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), inputStream, filename, 0L);
		} finally {
			tempFile.deleteOnExit();
		}
	}

	protected void deleteFilesFromInputFileByPattern(final String fileExtension) throws ResourceNotFoundException {
		productInputFileService.deleteFilesByPattern(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), fileExtension);
	}

}
