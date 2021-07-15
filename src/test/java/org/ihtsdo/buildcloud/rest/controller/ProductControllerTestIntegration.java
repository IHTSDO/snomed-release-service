package org.ihtsdo.buildcloud.rest.controller;

import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class ProductControllerTestIntegration extends AbstractControllerTest {

	private IntegrationTestHelper integrationTestHelper;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc, "ProductControllerTest");
	}

	@Test
	public void testPublishUpload() throws Exception {
		//publish test file
		integrationTestHelper.publishFile("/three_readmes.zip", getClass(), HttpStatus.CREATED);

		//publish test file a second time - expect rejection
		integrationTestHelper.publishFile("/three_readmes.zip", getClass(),HttpStatus.CONFLICT);

		//publish non zip file which we expect to fail
		integrationTestHelper.publishFile("/invalid-file-test-defn.xsd", getClass(), HttpStatus.BAD_REQUEST);
	}



}
