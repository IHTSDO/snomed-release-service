package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletException;

public class ProductControllerTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	private IntegrationTestHelper integrationTestHelper;

	@Test
	public void testPublishUpload() throws Exception {
		integrationTestHelper.loginAsManager();

		//publish test file
		integrationTestHelper.publishFile("/three_readmes.zip", getClass(),HttpStatus.NO_CONTENT);
		
		//publish non zip file which we expect to fail
		integrationTestHelper.publishFile("/invalid-file-test-defn.xsd", getClass(), HttpStatus.BAD_REQUEST);

	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "ProductControllerTest");
		((TestS3Client) s3Client).deleteBuckets();
	}

}
