package org.ihtsdo.buildcloud.core.service.classifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.ihtsdo.buildcloud.core.service.classifier.ClassificationServiceRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Before;
import org.junit.Test;

public class ClassificationServiceRestClientTestHarness {
	
	private ClassificationServiceRestClient client;
	
	private final String classificationServiceUrl = "http://localhost:8081/classification-service";
	private final String userName = "classification";
	private final String password = "classification";
	
	@Before
	public void setUp() throws BusinessServiceException {
		client = new ClassificationServiceRestClient(classificationServiceUrl, userName, password);
		client.setTimeoutInSeconds(300);
	}
	
	@Test
	public void testClassification() throws Exception {
		assertTrue(client.getTimeoutInSeconds() > 0);
		//Put RF2Delta.zip in folder release under project data-service
		File rf2DeltaZipFile = new File("release/RF2DeltaWithAxiom.zip");
		assertTrue(rf2DeltaZipFile.exists());
		File result = client.classify(rf2DeltaZipFile, "SnomedCT_InternationalRF2_MEMBER_20190131T120000Z.zip", null);
		assertNotNull(result);
		System.out.println("Classification result is saved at:" + result.getAbsolutePath());
	}
	
	
	@Test
	public void testEmptyDelta() throws Exception {
		assertTrue(client.getTimeoutInSeconds() > 0);
		//Put RF2Delta.zip in folder release under project data-service
		File rf2DeltaZipFile = new File("release/Empty_RF2Delta.zip");
		assertTrue(rf2DeltaZipFile.exists());
		File result = client.classify(rf2DeltaZipFile, "SnomedCT_USExtensionRF2_PRODUCTION_20180901T120000Z.zip", "SnomedCT_InternationalRF2_MEMBER_20190131T120000Z.zip");
		assertNotNull(result);
		System.out.println("Classification result is saved at:" + result.getAbsolutePath());
	}
}
