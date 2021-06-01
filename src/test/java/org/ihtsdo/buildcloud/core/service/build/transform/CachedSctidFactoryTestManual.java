package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.Date;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.transform.CachedSctidFactory;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClientImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CachedSctidFactoryTestManual {

	private CachedSctidFactory cachedSctidFactory;
	private IdServiceRestClient idRestClient;
	private final String url = "http://localhost:8080/api/";
	private final String userName = "userName";
	private final String password = "password";

	@Before
	public void setUp() throws Exception {
		idRestClient = new IdServiceRestClientImpl(url, userName, password);
		idRestClient.logIn();
		cachedSctidFactory = new CachedSctidFactory(RF2Constants.INTERNATIONAL_NAMESPACE_ID, "20150131", new Date().toString(), idRestClient,1, 10);
	}

	@Test
	public void testGetSCTID() throws Exception {
		Assert.assertNotNull(cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123"));
	}

}
