package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.BuildDAOImpl;
import org.ihtsdo.buildcloud.core.service.build.transform.PseudoUUIDGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClientOfflineDemoImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.TestS3Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest(properties = "spring.scheduler.enabled=false")
@Transactional
public abstract class AbstractTest {
	@Autowired
	private S3Client s3Client;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO buildDAO;

	@BeforeEach
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
		// Keep BuildDAOImpl's in-memory cache in sync with the freshly-reset S3 test store
		if (buildDAO instanceof BuildDAOImpl impl) {
			impl.clearBuildIdsCache();
		}
	}

	@AfterEach
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
