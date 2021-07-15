package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.service.build.transform.PseudoUUIDGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClientOfflineDemoImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractTest {
	@Autowired
	private S3Client s3Client;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Before
	public void setup() throws Exception {
		if (uuidGenerator instanceof PseudoUUIDGenerator) {
			((PseudoUUIDGenerator)uuidGenerator).reset();
		}
		if ( idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl)idRestClient).reset();
		}
		if (s3Client instanceof TestS3Client) {
			((TestS3Client) s3Client).freshBucketStore();
		}
	}

	@After
	public void tearDown() throws IOException {
		if (uuidGenerator instanceof PseudoUUIDGenerator) {
			((PseudoUUIDGenerator) uuidGenerator).reset();
		}
		if (idRestClient instanceof IdServiceRestClientOfflineDemoImpl) {
			((IdServiceRestClientOfflineDemoImpl) idRestClient).reset();
		}
		((TestS3Client) s3Client).freshBucketStore();
	}
}
