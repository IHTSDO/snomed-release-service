package org.ihtsdo.idgeneration;

import java.util.UUID;

import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.idgeneration.IdAssignmentImpl;
import org.junit.Test;

public class IdAssignmentImplTextDefinitionPreReleaseTest {

	@Test
	// TODO: This test can not fail because the exception is just printed. Fix inherited test.
	public void testApi() {
		try {
			IdAssignmentBI idAssignment = new IdAssignmentImpl();

			//Individual creation for any component
			UUID componentUuid = UUID.randomUUID();
			Long newSctId = idAssignment.createSCTID(componentUuid, null, "01", null, null, null);
			System.out.println("New SCTID: " + newSctId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
