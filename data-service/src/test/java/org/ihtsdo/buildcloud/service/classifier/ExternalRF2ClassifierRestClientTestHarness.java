package org.ihtsdo.buildcloud.service.classifier;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.ihtsdo.buildcloud.service.classifier.ExternalRF2ClassifierRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Before;
import org.junit.Test;

import us.monoid.json.JSONException;

public class ExternalRF2ClassifierRestClientTestHarness {
	
	private ExternalRF2ClassifierRestClient client;
	
	private String classificationServiceUrl = "http://localhost:8081/classification-service";
	private String userName = "classification";
	private String password = "classification";
	
	@Before
	public void setUp() throws BusinessServiceException {
		client = new ExternalRF2ClassifierRestClient(classificationServiceUrl, userName, password);
	}
	
	@Test
	public void testClassification() throws Exception {
		//Add RF2Delta.zip file in test/resources/org/ihtsdo/buildcloud/service/classifier
		File rf2DeltaZipFile = new File(ExternalRF2ClassifierRestClientTestHarness.class.getResource("RF2Delta.zip").toURI());
		File result = client.classify(rf2DeltaZipFile, "SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z.zip");
		assertNotNull(result);
	}

}
