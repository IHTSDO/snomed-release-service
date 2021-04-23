package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.config.TestConfig;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClientOfflineDemoImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
public abstract class AbstractControllerTest {

	public static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

	public static final String ROOT_URL = "http://localhost";

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Value("${srs.build.bucketName}")
	private String buildBucketName;

	@Value("${srs.build.published-bucketName}")
	private String publishedBucketName;

	@Before
	public void setup() throws Exception {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilter(filter, "/*").build();
		Assert.assertNotNull(mockMvc);
		if (uuidGenerator instanceof PesudoUUIDGenerator) {
			((PesudoUUIDGenerator)uuidGenerator).reset();
		}
		if ( idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl)idRestClient).reset();
		}
		if (s3Client instanceof TestS3Client) {
			final TestS3Client testS3Client = (TestS3Client) s3Client;
			testS3Client.freshBucketStore();
			testS3Client.createBucket(buildBucketName);
			testS3Client.createBucket(publishedBucketName);
		}
		
	}

	@After
	public void tearDown() {
		if (uuidGenerator instanceof PesudoUUIDGenerator) {
			((PesudoUUIDGenerator)uuidGenerator).reset();
		}
		if ( idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl)idRestClient).reset();
		}
	}
}
