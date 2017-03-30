package org.ihtsdo.buildcloud.service.identifier.client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class IdServiceRestClientImpTestManual {
	
	private static final String UUID_TWO = "ae4de89d-65ce-475c-80e1-a4f92212cd57";
	private static final String UUID_ONE = "72bc1612-a773-4be9-8eab-b6d48b2ffc9f";
	private static IdServiceRestClient client;
	private static final String idServiceApiUrl = "https://dev-cis.ihtsdotools.org/api";
	private static final String userName ="username";
	private static final String password ="username";
	
	@BeforeClass
	public static void  setUp() throws Exception {
		client = new IdServiceRestClientImpl(idServiceApiUrl, userName, password);
		client.logIn();
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		client.logOut();
	}
	@Test
	public void testCreateSctId() throws Exception {
		Long sctId = client.getOrCreateSctId(UUID.fromString("E2F7B688-396E-4E84-A78B-6802698EB309"), new Integer("0"), "00","testing");
		assertNotNull(sctId);
		System.out.println("Sctid:" + sctId);
		assertEquals( new Long("715155004"), sctId);
	}
	@Test
	public void testCreateSctids() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString(UUID_ONE));
		uuids.add(UUID.fromString(UUID_TWO));
		Map<UUID,Long> result = client.getOrCreateSctIds(uuids, new Integer("0"), "00", "testing");
		assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " SCTID:" + result.get(uuid));
			assertNotNull(result.get(uuid));
		}
	}
	
	@Test
	public void testCreateCtv3Ids() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString(UUID_ONE));
		uuids.add(UUID.fromString(UUID_TWO));
		Map<UUID,String> result = client.getOrCreateSchemeIds(uuids, SchemeIdType.CTV3ID, "testing");
		assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " CTV3ID:" + result.get(uuid));
			assertNotNull(result.get(uuid));
		}
	}
	
	@Test
	public void testCreateSnomedIds() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString(UUID_ONE));
		uuids.add(UUID.fromString(UUID_TWO));
		Map<UUID,String> result = client.getOrCreateSchemeIds(uuids, SchemeIdType.SNOMEDID, "testing");
		assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " SnomedId:" + result.get(uuid));
			assertNotNull(result.get(uuid));
		}
	}
	
	@Test(expected=RestClientException.class)
	public void testPublishSctIds() throws Exception {
		List<Long> sctids = new ArrayList<>();
		sctids.add(new Long("714141004"));
		sctids.add(new Long("714139000"));
		assertTrue(client.publishSctIds(sctids, new Integer("0"), "testing"));
	}
	
	@Test(expected=RestClientException.class)
	public void testPublishSnomedIds() throws Exception {
		List<String> snomedIds = new ArrayList<>();
		snomedIds.add("R-FF605");
		snomedIds.add("R-FF609");
		assertTrue(client.publishSchemeIds(snomedIds,SchemeIdType.SNOMEDID, "publish testing"));
	}
	
	
	@Test(expected=RestClientException.class)
	public void testPublishCtv3Ids() throws Exception {
		List<String> ctv3Ids = new ArrayList<>();
		ctv3Ids.add("XUsHT");
		ctv3Ids.add("XUsfV");
		
		assertTrue(client.publishSchemeIds(ctv3Ids, SchemeIdType.CTV3ID, "publish testing"));
	}
	
	@Test
	public void testGetSctIdStatusMap() throws Exception {
		List<Long> sctids = new ArrayList<>();
		sctids.add(new Long("714141004"));
		sctids.add(new Long("714139000"));
		Map<Long, String> result = client.getStatusForSctIds(sctids);
		assertEquals(sctids.size(), result.keySet().size());
		assertTrue(result.keySet().containsAll(sctids));
		for (Long key : result.keySet()) {
			System.out.println("sctId:" + key + " status:" + result.get(key));
		}
	}
	
	@Test
	public void testGetSnomedIdStatusMap() throws Exception {
		List<String> snomedIds = new ArrayList<>();
		snomedIds.add("R-FF605");
		snomedIds.add("R-FF609");
		Map<String, String> result = client.getStatusForSchemeIds(SchemeIdType.SNOMEDID, snomedIds);
		assertEquals(snomedIds.size(), result.keySet().size());
		assertTrue(result.keySet().containsAll(snomedIds));
		for (String id : result.keySet()) {
			System.out.println("ID:" + id + " status:" + result.get(id));
		}
	}
	
	@Test
	public void testGetCtv3IdStatusMap() throws Exception {
		List<String> ctv3Ids = new ArrayList<>();
		ctv3Ids.add("XUsHT");
		ctv3Ids.add("XUsfV");
		Map<String, String> result = client.getStatusForSchemeIds(SchemeIdType.CTV3ID, ctv3Ids);
		assertEquals(ctv3Ids.size(), result.keySet().size());
		assertTrue(result.keySet().containsAll(ctv3Ids));
		for (String id : result.keySet()) {
			System.out.println("ID:" + id + " status:" + result.get(id));
		}
	}
	@Test
	public void testRegisterSctIds() throws Exception {
		List<Long> sctids = new ArrayList<>();
		sctids.add(new Long("1484571000005112"));
		sctids.add(new Long("4893241000005115"));
		List<Long> result = client.registerSctIds(sctids, null,new Integer("1000005"), "testing");
		assertEquals(sctids.size(), result.size());
		assertTrue(result.containsAll(sctids));
	}
}

