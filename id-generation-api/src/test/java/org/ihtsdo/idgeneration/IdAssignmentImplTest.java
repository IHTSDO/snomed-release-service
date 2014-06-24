package org.ihtsdo.idgeneration;

import org.ihtsdo.idgeneration.IdAssignmentBI.IDENTIFIER;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class IdAssignmentImplTest {

	@Test
	public void testApi() throws Exception {
		IdAssignmentBI idAssignment = new IdAssignmentImpl("http://mgr.servers.aceworkspace.net:50008/axis2/services/id_generator");

		//Individual creation for any component
		UUID componentUuid = UUID.fromString("c83bafaa-ec84-55c1-b1c9-1234a25fe706");
		Long newSctId = idAssignment.createSCTID(componentUuid, 0, "01", "20110111", "TEST EXECUTION", "12345");
		System.out.println("New SCTID: " + newSctId);
		Long sctid = idAssignment.getSCTID(UUID.fromString("c83bafaa-ec84-55c1-b1c9-1234a25fe706"));
		System.out.println("##############  " + sctid);

		//Batch creation for any component
		List<UUID> componentUuidList = new ArrayList<UUID>();
		for (int i = 0; i < 2500; i++) {
			componentUuidList.add(UUID.randomUUID());
		}
		HashMap<UUID, Long> listOfNewSctIds = idAssignment.createSCTIDList(componentUuidList , 0, "01", "20110111","TEST EXECUTION", "12345");
		for (UUID uuid : listOfNewSctIds.keySet()) {
			System.out.println(uuid + " - "  + listOfNewSctIds.get(uuid));
		}

		System.out.println(listOfNewSctIds.size());

		//Individual creation for concept
		HashMap<IDENTIFIER, String> resultMap = idAssignment.createConceptIds(UUID.randomUUID(), "G-61EF", 0, "00", "20110111", "TEST EXECUTION", "12345");
		System.out.println("\n");
		for (IDENTIFIER loopIdent : resultMap.keySet()) {
			System.out.println("New Concept Identifier: " + loopIdent + " Value: " + resultMap.get(loopIdent));
		}

		// Batch creation for concept
		HashMap<UUID, String> componentUUIDandParentSnomedId = createComponentUUIDandParentSnomedID();
		HashMap<UUID, HashMap<IDENTIFIER, String>> result = idAssignment.createConceptIDList(componentUUIDandParentSnomedId, 0, "00", "20110111", "TEST EXECUTION", "12345");
		System.out.println();
		for (UUID loopUuid : result.keySet()) {
			HashMap<IDENTIFIER, String> loopHashMap = result.get(loopUuid);
			System.out.println("Batch Concept UUID: " + loopUuid);
			for (IDENTIFIER loopIdent : loopHashMap.keySet()) {
				System.out.println("\tIdentifier: " + loopIdent + " Value: " + loopHashMap.get(loopIdent));
			}
		}
	}

	private HashMap<UUID, String> createComponentUUIDandParentSnomedID() {
		HashMap<UUID, String> componentUUIDandParentSnomedId = new HashMap<UUID, String>();
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "T-A12EF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "G-A2EF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "G-61EF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "P3-D1EF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "T-A1BEF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "T-C12EF");
		componentUUIDandParentSnomedId.put(UUID.randomUUID(), "T-B12EF");
		return componentUUIDandParentSnomedId;
	}

}
