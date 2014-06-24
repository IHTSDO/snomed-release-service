package org.ihtsdo.idgeneration;

import org.junit.Test;

import java.util.UUID;

public class IdAssignmentImplTextDefinitionPreReleaseTest {

	@Test
	public void testApi() throws Exception {
		IdAssignmentBI idAssignment = new IdAssignmentImpl("http://mgr.servers.aceworkspace.net:50008/axis2/services/id_generator");

		//Individual creation for any component
		UUID componentUuid = UUID.randomUUID();
		Long newSctId = idAssignment.createSCTID(componentUuid, null, "01", null, null, null);
		System.out.println("New SCTID: " + newSctId);
	}

}
