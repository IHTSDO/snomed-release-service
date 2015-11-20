package org.ihtsdo.buildcloud.service.identifier.client;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class IdServiceRestClientImpTestManual {
	
	private IdServiceRestClient client;
	private String idServiceApiUrl = "http://162.243.20.236:3000/api";
	private String userName ="userName";
	private String password ="Password";
	
	@Before
	public void setUp() throws Exception {
		client = new IdServiceRestClientImpl(idServiceApiUrl, userName, password);
		client.logIn();
	}
	
	@After
	public void tearDown() throws Exception {
		client.logOut();
	}
	@Test
	public void testCreateSctId() throws Exception {
		
		Long sctId = client.getOrCreateSctId(UUID.fromString("E2F7B688-396E-4E84-A78B-6802698EB309"), new Integer("0"), "00","testing");
		Assert.assertNotNull(sctId);
		System.out.println("Sctid:" + sctId);
		Assert.assertEquals( new Long("714141004"), sctId);
	}
	@Test
	public void testCreateSctids() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString("72bc1612-a773-4be9-8eab-b6d48b2ffc9f"));
		uuids.add(UUID.fromString("ae4de89d-65ce-475c-80e1-a4f92212cd57"));
		Map<UUID,Long> result = client.getOrCreateSctIds(uuids, new Integer("0"), "00", "testing");
		Assert.assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " SCTID:" + result.get(uuid));
			Assert.assertNotNull(result.get(uuid));
		}
	}
	
	@Test
	public void testCreateCtv3Ids() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString("72bc1612-a773-4be9-8eab-b6d48b2ffc9f"));
		uuids.add(UUID.fromString("ae4de89d-65ce-475c-80e1-a4f92212cd57"));
		Map<UUID,String> result = client.getOrCreateSchemeIds(uuids, SchemeIdType.CTV3ID, "testing");
		Assert.assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " CTV3ID:" + result.get(uuid));
			Assert.assertNotNull(result.get(uuid));
		}
	}
	
	@Test
	public void testCreateSnomedIds() throws Exception {
		List<UUID> uuids = new ArrayList<>();
		uuids.add(UUID.fromString("72bc1612-a773-4be9-8eab-b6d48b2ffc9f"));
		uuids.add(UUID.fromString("ae4de89d-65ce-475c-80e1-a4f92212cd57"));
		Map<UUID,String> result = client.getOrCreateSchemeIds(uuids, SchemeIdType.SNOMEDID, "testing");
		Assert.assertEquals(uuids.size(), result.size());
		for ( UUID uuid : result.keySet()) {
			System.out.println("UUID:" + uuid + " SnomedId:" + result.get(uuid));
			Assert.assertNotNull(result.get(uuid));
		}
	}
}

