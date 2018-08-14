package org.ihtsdo.buildcloud.service.classifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Before;
import org.junit.Test;

public class ExternalRF2ClassifierRestClientTestHarness {
	
	private ExternalRF2ClassifierRestClient client;
	
	private String classificationServiceUrl = "http://localhost:8081/classification-service";
	private String userName = "classification";
	private String password = "classification";
	
	@Before
	public void setUp() throws BusinessServiceException {
		client = new ExternalRF2ClassifierRestClient(classificationServiceUrl, userName, password);
		client.setTimeoutInSeconds(300);
	}
	
	@Test
	public void testClassification() throws Exception {
		assertTrue(client.getTimeoutInSeconds() > 0);
		//Put RF2Delta.zip in folder release under project data-service
		File rf2DeltaZipFile = new File("release/RF2Delta.zip");
		assertTrue(rf2DeltaZipFile.exists());
		File result = client.classify(rf2DeltaZipFile, Arrays.asList("SnomedCT_InternationalRF2_PRODUCTION_20180131T120000Z.zip"));
		assertNotNull(result);
		System.out.println("Classification result is saved at:" + result.getAbsolutePath().toString());
	}
	
	
	@Test
	public void testEmptyDelta() throws Exception {
		assertTrue(client.getTimeoutInSeconds() > 0);
		//Put RF2Delta.zip in folder release under project data-service
		File rf2DeltaZipFile = new File("release/Empty_RF2Delta.zip");
		assertTrue(rf2DeltaZipFile.exists());
		File result = client.classify(rf2DeltaZipFile, Arrays.asList("xSnomedCT_InternationalRF2_ALPHA_20180731T120000Z.zip", "SnomedCT_InternationalRF2_PRODUCTION_20180131T120000Z.zip"));
		assertNotNull(result);
		System.out.println("Classification result is saved at:" + result.getAbsolutePath().toString());
	}
}
