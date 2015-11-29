package org.ihtsdo.buildcloud.service.identifier.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IdServiceClientConcurrencyTestManual {


	private static final String COMMENT = "Testing";
	private static final String MODULE_ID = "12345";
	private static final String EXECUTION_ID = "TEST EXECUTION";
	private static final String RELEASE_ID = "20110111";
	private static final String PARTITION_ID = "00";
	private static final int NAMES_SPACE_ID = 0;
	private static final String LOCAL_HOST = "http://uat-cis.ihtsdotools.org:3000/api";
	private static final String USER_NAME = "username";
	private static final String PASSWORD = "password";
	private static IdServiceRestClient idGen;
	
	public static void main(final String[] args) throws Exception{

		idGen = new IdServiceRestClientImpl(LOCAL_HOST,USER_NAME,PASSWORD);
		idGen.logIn();
		
		//testing SCT id creation
		final UUID uuid = UUID.randomUUID();
		System.out.println("UUID:" + uuid);
		createSctIdInAThread(uuid);
		createSctIdInAThread(uuid);
//		List<UUID> uuids = Arrays.asList(uuid);
//		createSctIdsInAThread(uuids);
//		createSctIdsInAThread(uuids);
//
//
//		final UUID newUuid = UUID.randomUUID();
//		System.out.println("Child UUID:" + newUuid);
//		createSctIdInAThread(newUuid);
//
//		createSnomedIdsInAThread(uuids);
//		createSnomedIdsInAThread(uuids);
//		createCtv3IdsInAThread(uuids);
//		createCtv3IdsInAThread(uuids);
		idGen.logOut();
	}




	private static void createSnomedIdsInAThread(final List<UUID> uuids) {
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//create a different idgen client instance
					final IdServiceRestClient idGen = new IdServiceRestClientImpl(LOCAL_HOST,USER_NAME,PASSWORD);
					idGen.logIn();
					Map<UUID,String> result = idGen.getOrCreateSchemeIds(uuids, SchemeIdType.SNOMEDID, COMMENT);
					for (UUID uuid : result.keySet()) {
						System.out.println("Thread:" + Thread.currentThread().getId()  + " created SNOMED ID:" + result.get(uuid) + " for UUID:" + uuid);
					}
					idGen.logOut();
				} catch (final Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
		t.start();
	}


	private static void createCtv3IdsInAThread(final List<UUID> uuids) {
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//comment this out to testing multiple connections.
//					final IdServiceRestClient idGen = new IdServiceRestClientImpl(LOCAL_HOST,USER_NAME,PASSWORD);
//					idGen.logIn();
					final Map<UUID,String> ctv3IdMap = idGen.getOrCreateSchemeIds(uuids, SchemeIdType.CTV3ID, COMMENT);
					for (UUID uuid : ctv3IdMap.keySet()) {
						System.out.println("Thread:" + Thread.currentThread().getId()  + " created CTV3 ID:" + ctv3IdMap.get(uuid) + " for UUID:" + uuid);
					}
//					idGen.logOut();
				} catch (final Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
		t.start();
	}

	private static void  createSctIdInAThread(final UUID componentUuid) {
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final long id = Thread.currentThread().getId();
					System.out.println("Thread running:" + id);
					final IdServiceRestClient idGen = new IdServiceRestClientImpl(LOCAL_HOST,USER_NAME,PASSWORD);
					idGen.logIn();
					final Long sctId = idGen.getOrCreateSctId(componentUuid, NAMES_SPACE_ID, PARTITION_ID, "testing");
					System.out.println("Thread:" + id + " created SCT ID:" + sctId + " for UUID:" + componentUuid);
					idGen.logOut();
				} catch (final Exception e) {
					e.printStackTrace();
					throw new RuntimeException("Failed to create sctID!", e);
				}
			}
		});
		t.start();
	}

	private static void createSctIdsInAThread(final List<UUID> uuids) {
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final long id = Thread.currentThread().getId();
					System.out.println("Thread running:" + id);
//					final IdServiceRestClient idGen = new IdServiceRestClientImpl(LOCAL_HOST,USER_NAME,PASSWORD);
					final Map<UUID,Long> result = idGen.getOrCreateSctIds(uuids, NAMES_SPACE_ID,PARTITION_ID, COMMENT);
					for (UUID uuid : result.keySet()) {
						System.out.println("SCT ID created:" + result.get(uuid) + " by thread:" + id);
					}
				} catch (final Exception e) {
					e.printStackTrace();
					throw new RuntimeException("Failed to create!", e);
				}
			}
		});
		t.start();
	}


	private static void delay(final long delay) {
		try {
			Thread.sleep(delay);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

}
