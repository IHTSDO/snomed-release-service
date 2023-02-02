package org.ihtsdo.buildcloud.core.dao;

import org.apache.commons.codec.binary.Base64;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

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

	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		product = productDAO.find(1L);
		final Date creationTime = new GregorianCalendar(2014, 1, 4, 10, 30, 1).getTime();
		build = new Build(creationTime, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
		buildDAO.save(build);
		buildId = build.getId();
	}

	@Test
	public void testFind() {
		// saved build
		Build foundBuild = buildDAO.find(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), buildId, null, null, null, null);
		assertNotNull(foundBuild);
		assertEquals("2014-02-04T10:30:01", foundBuild.getCreationTime());
		assertEquals(Build.Status.PENDING, foundBuild.getStatus());

		// no existing build id
		foundBuild = buildDAO.find(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), "2014-02-04T10:30:02", null, null, null, null);
		assertNull(foundBuild);
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
		assertArrayEquals(md5BytesExpected, md5BytesReceived);
		
		//Now lets see if we can get that file back out again
		final InputStream is = buildDAO.getOutputFileInputStream(build, TEST_FILE_NAME);
		assertNotNull(is);
	}

	@Test
	public void findAllDescPage_ShouldReturnExpectedPage_WhenRequestingAll() throws IOException, InterruptedException {
		// given
		createBuild();
		createBuild();
		createBuild();
		Date build4 = createBuild();

		// when
		BuildPage<Build> result = buildDAO.findAll(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), null, null, null, null, BuildService.View.ALL_RELEASES, PageRequest.of(0, 10));

		// then
		assertEquals(5, result.getTotalElements());

		// then
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		Sort.Order order = new Sort.Order(Sort.Direction.DESC, "creationTime");
		result = buildDAO.findAll(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), null, null, null, null, BuildService.View.ALL_RELEASES, PageRequest.of(0, 10, Sort.by(order)));
		String lhs = simpleDateFormat.format(build4);
		String rhs = result.getContent().get(0).getCreationTime();
		assertEquals(lhs, rhs);

		order = new Sort.Order(Sort.Direction.ASC, "creationTime");
		result = buildDAO.findAll(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), null, null, null, null, BuildService.View.ALL_RELEASES, PageRequest.of(0, 10, Sort.by(order)));
		lhs = buildId;
		rhs = result.getContent().get(0).getCreationTime();
		assertEquals(lhs, rhs);
	}

	@Test
	public void findAllDescPage_ShouldReturnExpectedPage_WhenRequestingSubPage() throws IOException, InterruptedException {
		// given
		Date build1 = createBuild();
		Date build2 = createBuild();
		Date build3 = createBuild();
		Date build4 = createBuild();

		// when
		BuildPage<Build> result = buildDAO.findAll(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), null, null, null, null, BuildService.View.ALL_RELEASES, PageRequest.of(3, 1));

		// then
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String lhs = simpleDateFormat.format(build1);
		String rhs = result.getContent().get(0).getCreationTime();
		assertEquals(lhs, rhs);
	}

	private Date createBuild() throws InterruptedException, IOException {
		Thread.sleep(1000); // Sleep for different time
		Date date = new Date();
		Build build4 = new Build(date, product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), product.getBuildConfiguration(), product.getQaTestConfig());
		buildDAO.save(build4);

		return date;
	}

}
