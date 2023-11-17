package org.ihtsdo.buildcloud.core.service.validation.precondition;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.dao.InputFileDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.core.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.InputFileService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public abstract class PreconditionCheckTest extends AbstractTest {

	@Autowired
	protected InputFileService inputFileService;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private TestUtils testUtils;

	protected Product product;

	protected Build build = null;

	protected PreconditionManager manager;

	protected static final String JULY_RELEASE = "20140731";

	private static int buildIdx = 0;

	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionCheckTest.class);

	private File tempDir;

	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		product = productDAO.find(1L);
		if(product.getBuildConfiguration() == null) {
			product.setBuildConfiguration(new BuildConfiguration());
		}
		if (product.getBuildConfiguration().getEffectiveTime() == null) {
			product.getBuildConfiguration().setEffectiveTime(RF2Constants.DATE_FORMAT.parse(JULY_RELEASE));
		}
		tempDir = Files.createTempDirectory("temp-test").toFile();
	}

	@AfterEach
	public void tearDown() throws IOException {
		super.tearDown();
		if (tempDir != null && tempDir.exists()) {
			Arrays.stream(tempDir.listFiles()).forEach(file -> {
				if (file.exists()) {
					file.delete();
				}
			});
			tempDir.delete();
		}
	}


	protected void createNewBuild(Boolean isBeta) throws IOException {
		final Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, buildIdx++).getTime();
		build = new Build(creationTime, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
		if (Boolean.TRUE.equals(isBeta)) {
			build.getConfiguration().setBetaRelease(true);
		}

		// Because we're working with a unit test, that build will probably already exist on disk, so wipe
		testUtils.scrubBuild(build);
	}

	protected PreConditionCheckReport runPreConditionCheck(final Class<? extends PreconditionCheck> classUnderTest) throws IOException {

		if (build == null) {
			createNewBuild(false);
		}

		// Create a manager for this test
		final List<PreConditionCheckReport> report = manager.runPreconditionChecks(build);
		assertNotNull(report);

		final PreConditionCheckReport testResult = report.get(0); // Get the first test run

		final String testName = testResult.getPreConditionCheckName();
		assertEquals(classUnderTest.getSimpleName(), testName);

		// If it's a fail, we'll debug that message just for testing purposes
		if (State.PASS != testResult.getResult()) {
			LOGGER.warn("Test {} Reported {}", testName, testResult.getMessage());
		}
		return testResult;
	}

	protected void loadManifest(final String filename) throws IOException, DecoderException {
		loadManifest(filename, false);
	}

	protected void loadManifest(final String filename, final boolean isBetaRelease) throws IOException, DecoderException {
		if (filename != null) {
			createNewBuild(isBetaRelease);
			final String testFilePath = getClass().getResource(filename).getFile();
			final File testManifest = new File(testFilePath);
			inputFileDAO.putManifestFile(build.getReleaseCenterKey(), build.getProductKey(), build.getId(), new FileInputStream(testManifest), testManifest.getName(), testManifest.length());
		}
	}

	/**
	 * call before loadManifest.
	 *
	 * @param filename
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	protected void addEmptyFileToInputDirectory(final String filename) throws ResourceNotFoundException, IOException {
		final File tempFile = new File(tempDir, filename);
		if (!tempFile.createNewFile()) {
			throw new IllegalStateException("Failed to create temp file " + filename);
		}
		buildDAO.putInputFile(build, tempFile, false);
	}

}
