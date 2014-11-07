package org.ihtsdo.buildcloud.service.execution.transform;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.idgen.ws.CreateCTV3IDListFaultException;
import org.ihtsdo.idgen.ws.CreateSnomedIDListFaultException;
import org.junit.Before;
import org.junit.Test;

public class ParentSctIdFinderTest {

	private static final String SCT2_STATED_RELATIONSHIP_DELTA = "sct2_StatedRelationship_Delta_INT_20140731.txt";
	private ParentSctIdFinder finder;
	
	@Before
	public void setUp() {
		finder = new ParentSctIdFinder();
	}
	@Test
	public void testFindParentSctIdUsingStatedRelationshipFile() throws TransformationException {
		
//		final List<Long> sourceIds = new ArrayList<>();
//		sourceIds.add( new Long(703527003));
//		sourceIds.add(new Long(703426005));
//		final InputStream input = ParentSctIdFinderTest.class.getResourceAsStream(SCT2_STATED_RELATIONSHIP_DELTA);
//		final Map<Long, Long> result = finder.getParentSctIdFromStatedRelationship(input, sourceIds);
//		assertEquals(237995002, result.get(new Long(703527003)).longValue());
//		assertEquals( 118594004, result.get(new Long(703426005)).longValue());
//		//toremove
		final IdAssignmentBIOfflineDemoImpl demo = new IdAssignmentBIOfflineDemoImpl();
		Map<UUID, String> test;
		try {
			test = demo.createCTV3IDList(Arrays.asList(UUID.randomUUID(),UUID.randomUUID()));
			for( final String ctv3Id : test.values()){
				System.out.println("CTV3ID:" + ctv3Id);
			}
		} catch (final RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final CreateCTV3IDListFaultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final Long[][] sctIdAndParentSctIdArray = new Long[1][2];
		sctIdAndParentSctIdArray[0][0] = 12334L;
		try {
			final Map<Long,String> snomedIdResult = demo.createSNOMEDIDList(sctIdAndParentSctIdArray);
			for(final String snomedId : snomedIdResult.values()) {
				System.out.println("SNOMEDID:" + snomedId);
			}
		} catch (RemoteException | CreateSnomedIDListFaultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
