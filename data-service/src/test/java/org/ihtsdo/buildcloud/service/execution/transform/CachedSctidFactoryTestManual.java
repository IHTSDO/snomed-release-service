package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.idgeneration.IdAssignmentImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class CachedSctidFactoryTestManual {

	private CachedSctidFactory cachedSctidFactory;
	private IdAssignmentBI idAssignmentBI;

	@Before
	public void setUp() throws Exception {
		idAssignmentBI = new IdAssignmentImpl("http://cu026.servers.aceworkspace.net:7080/axis2/services/id_generator");
		cachedSctidFactory = new CachedSctidFactory(TransformationService.INTERNATIONAL_NAMESPACE_ID, "20150131", new Date().toString(), idAssignmentBI, 3, 10);
	}

	@Test
	public void testGetSCTID() throws Exception {
		Assert.assertNotNull(cachedSctidFactory.getSCTID("e568b6b6-1869-4adb-99ea-039d076f64f0", "1", "123"));
	}

}
