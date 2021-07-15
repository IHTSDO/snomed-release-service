package org.ihtsdo.buildcloud.core.dao;

import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;

@Transactional
public class BuildDAOImplTest extends AbstractTest {
	
	public static final String TEST_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";

	@Autowired
	protected BuildDAOImpl buildDAO;

	@Autowired
	protected ProductDAO productDAO;

	private Product product;

	private Build build;

	private String buildId;

	@Before
	public void setup() throws Exception {
		super.setup();
		product = productDAO.find(1L);
		final Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 1).getTime();
		build = new Build(creationTime, product);
		buildDAO.save(build);
		buildId = build.getId();
	}

	@Test
	public void testFind() {
		// saved build
		Build foundBuild = buildDAO.find(product, buildId, null, null, null, null);
		Assert.assertNotNull(foundBuild);
		Assert.assertEquals("2014-02-04T10:30:01", foundBuild.getCreationTime());
		Assert.assertEquals(Build.Status.PENDING, foundBuild.getStatus());

		// no existing build id
		foundBuild = buildDAO.find(product, "2014-02-04T10:30:02", null, null, null, null);
		Assert.assertNull(foundBuild);
	}
	
	
	@Test
	public void testPutOutputFile() throws Exception {

		//Leaving this as offline to remove external dependency, but set to true to check Amazon is happy with our MD5 Encoding.
		final String testFile = getClass().getResource("/org/ihtsdo/buildcloud/core/service/build/" + TEST_FILE_NAME).getFile();

		final boolean calcMD5 = true;
		final String md5Received = buildDAO.putOutputFile(build, new File(testFile), calcMD5);

		System.out.println(md5Received);

		//Amazon are expecting the md5 to be xWJD6+IqEtukiwI9rz4pNw==
		//Offline test is just going to return the MD5 input, so this test only makes sense in online mode.
		final byte[] md5BytesExpected = Base64.decodeBase64("xWJD6+IqEtukiwI9rz4pNw==");
		final byte[] md5BytesReceived =  Base64.decodeBase64(md5Received);
		Assert.assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		final InputStream is = buildDAO.getOutputFileInputStream(build, TEST_FILE_NAME);
		Assert.assertNotNull(is);
	}

}
