package org.ihtsdo.buildcloud.core.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.EnumSet;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.core.dao.BuildDAOImpl;
import org.ihtsdo.buildcloud.core.dao.ProductDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.TransformationException;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.rest.pojo.BuildRequestPojo;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class PublishServiceImpl2Test extends AbstractTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl2Test.class);

	@Autowired
	protected BuildDAOImpl buildDAO;

	@Autowired
	private ProductService productService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private PublishService publishService;

	@Autowired
	protected ProductDAO productDAO;

	@Autowired
	private S3Client s3Client;

	private TestEntityGenerator generator;

	Build build = null;

	private static final String TEST_FILENAME = "test.zip";

	private static final String BETA_TEST_FILENAME = "xBetaTest.zip";
	private String releaseCenterName;

	@BeforeEach
	public void setup() throws BusinessServiceException, IOException, NoSuchAlgorithmException, TransformationException {

		generator = new TestEntityGenerator();
		releaseCenterName = EntityHelper.formatAsBusinessKey(generator.releaseCenterShortNames[0]);

		//Packages get looked up using a product composite key (ie include the unique ID)
		//so first lets find the first product for a known product, and use that
		EnumSet<FilterOption> filterOptions = EnumSet.of(FilterOption.INCLUDE_LEGACY);
		Page<Product> page = productService.findAll(releaseCenterName, filterOptions, PageRequest.of(0, 10), false);
		Product product = page.getContent().get(0);
		product.getBuildConfiguration().setEffectiveTime(new Date());

		BuildRequestPojo buildRequest = new BuildRequestPojo();
		buildRequest.setLoadExternalRefsetData(false);
		buildRequest.setLoadTermServerData(false);
		buildRequest.setBuildName("Test");
		buildRequest.setEffectiveDate("20210731");
		build = buildService.createBuildFromProduct(releaseCenterName, product.getBusinessKey(), buildRequest, null, null);

		//Put a zip file into the build's output directory so we have something to publish.
		String testFile = getClass().getResource("/" + TEST_FILENAME).getFile();
		buildDAO.putOutputFile(build, new File(testFile), false);
	}

	@AfterEach
	public void tearDown() throws IOException {
		((TestS3Client) s3Client).freshBucketStore();
	}

	@Test
	public void testPublishing() throws IOException, InterruptedException, BusinessServiceException {
		//Using separate threads to check second thread detects file already written.
		Thread a = runThread("one", publishService, build, null);
		// Give thread a a head start to ensure it enters the sync block first
		Thread.sleep(1);
		Thread b = runThread("two", publishService, build, EntityAlreadyExistsException.class);

		//Wait for both threads to finish.
		a.join();
		b.join();

		//Now call a final time and ensure same
		boolean expectedExceptionThrown = false;
		try {
			publishService.publishBuild(build, true, null);
		} catch (EntityAlreadyExistsException | DecoderException eaee) {
			expectedExceptionThrown = true;
		}

		assertTrue(expectedExceptionThrown, "Expected EntityAlreadyExistsException to have been thrown");

	}

	private static Thread runThread(final String threadName, final PublishService service, final Build build,
			final Class<?> expectedExceptionClass) {
		Thread thread = new Thread(() -> {
            try {
                service.publishBuild(build, true, null);
                LOGGER.info("Publishing complete in thread " + threadName);
            } catch (Exception e) {
                if (expectedExceptionClass == null) {
                    throw new RuntimeException("Unexpected exception thrown in thread " + threadName + " of PublishServiceTest2: ", e);
                }

                if (expectedExceptionClass != null && e.getClass() != expectedExceptionClass) {
                    throw new RuntimeException("Incorrect exception thrown in thread " + threadName + " of PublishServiceTest2: ", e);
                }
            }
        });
		thread.start();
		return thread;
	}

	@Test
	public void testPublishingAdhocFile() {
		InputStream inputStream = ClassLoader.getSystemResourceAsStream(BETA_TEST_FILENAME);
		long size = 1000;
		ReleaseCenter releaseCenter = new ReleaseCenter();
		releaseCenter.setShortName(releaseCenterName);
		try {
			publishService.publishAdHocFile(releaseCenter, inputStream, BETA_TEST_FILENAME, size, true);
		} catch (BusinessServiceException e) {
			e.printStackTrace();
			fail("Should not result in exception");
		}
	}

	@Test
	public void testPublishingOwlAxiom() {
		String fileToPublish = "test_axiom.zip";
		InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileToPublish);
		long size = 1000;
		ReleaseCenter releaseCenter = new ReleaseCenter();
		releaseCenter.setShortName(releaseCenterName);
		try {
			publishService.publishAdHocFile(releaseCenter, inputStream, fileToPublish, size, true);
		} catch (BusinessServiceException e) {
			e.printStackTrace();
			fail("Should not result in exception");
		}
	}
}
