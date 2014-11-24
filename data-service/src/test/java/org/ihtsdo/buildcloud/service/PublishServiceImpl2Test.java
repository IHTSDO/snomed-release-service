package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAOImpl;
import org.ihtsdo.buildcloud.dao.ProductDAO;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.entity.helper.TestEntityGenerator;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.test.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
@Transactional
public class PublishServiceImpl2Test extends TestEntityGenerator {

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

	Build build = null;

	private static final String TEST_FILENAME = "test.zip";

	@Before
	public void setup() throws BusinessServiceException, IOException {
		TestUtils.setTestUser();

		String releaseCenterName = EntityHelper.formatAsBusinessKey(releaseCenterShortNames[0]);

		//Tidyup in case we've run this test already today
		((TestS3Client)s3Client).freshBucketStore();

		//Packages get looked up using a product composite key (ie include the unique ID)
		//so first lets find the first product for a known product, and use that
		List<Product> products = productService.findAll(releaseCenterName, null);
		Product product = products.get(0);
		product.setEffectiveTime(new Date());
		build = buildService.createBuildFromProduct(releaseCenterName, product.getBusinessKey());

		//Put a zip file into the build's output directory so we have something to publish.
		String testFile = getClass().getResource("/" + TEST_FILENAME).getFile();
		buildDAO.putOutputFile(build, new File(testFile), false);
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
			publishService.publishBuild(build);
		} catch (EntityAlreadyExistsException eaee) {
			expectedExceptionThrown = true;
		}

		Assert.assertTrue("Expected EntityAlreadyExistsException to have been thrown", expectedExceptionThrown);

	}

	private static Thread runThread(final String threadName, final PublishService service, final Build build,
			final Class<?> expectedExceptionClass) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					service.publishBuild(build);
					LOGGER.info("Publishing complete in thread " +threadName );
				} catch (Exception e) {
					if (expectedExceptionClass == null) {
						throw new RuntimeException ("Unexpected exception thrown in thread " + threadName + " of PublishServiceTest2: ", e);
					}

					if (expectedExceptionClass != null && e.getClass() != expectedExceptionClass) {
						throw new RuntimeException ("Incorrect exception thrown in thread " + threadName + " of PublishServiceTest2: ", e);
					}
				}
			}
		};
		thread.start();
		return thread;
	}
}
