package org.ihtsdo.buildcloud.service.identifier.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.otf.rest.client.RestClientException;

public class IdServiceRestClientOfflineDemoImpl implements IdServiceRestClient {
	//Only for test purpose
	private static final String CTV3ID_PREFIX = "XUsW";

	private static final String SNOMED_ID_PREFIX = "R-F";

	private Long sctid;

	private static final String BOGUS_CHECK_DIGIT = "1";
	private static final NotImplementedException NOT_IMPLEMENTED_EXCEPTION = new NotImplementedException("Not implemented in the offline demo id generator.");

	private int snomedIdCounter;
	private int ctv3IdChar;

	public IdServiceRestClientOfflineDemoImpl() {
		reset();
	}

	@Override
	public Long getOrCreateSctId(UUID componentUuid, Integer namespaceId, String partitionId, String comment)
			throws RestClientException {
		return createNewId(partitionId);
	}

	@Override
	public HashMap<UUID, Long> getOrCreateSctIds(List<UUID> uuids, Integer namespaceId, String partitionId, String comment) {
		final HashMap<UUID, Long> map = new HashMap<>();
		for (final UUID uuid : uuids) {
			map.put(uuid, createNewId(partitionId));
		}
		return map;
	}

	public Long createNewId(final String partitionId) {
		sctid++;
		return Long.parseLong("" + sctid + partitionId + BOGUS_CHECK_DIGIT);
	}

	public void reset() {
		sctid = 800000L;
		snomedIdCounter = 1;
		ctv3IdChar = 64;
	}

	private Map<UUID, String> createCtv3Ids(final List<UUID> componentUuidList) {
		final Map<UUID, String> result =  new HashMap<>();
		for (final UUID uuid : componentUuidList) {
			do {
				ctv3IdChar++;
			} while(!Character.isAlphabetic(ctv3IdChar) && ctv3IdChar < 123);
			if (ctv3IdChar > 122) {
				ctv3IdChar = 64;
			}
			result.put(uuid, CTV3ID_PREFIX + Character.valueOf((char)(ctv3IdChar)));
		}
		return result;
	}


	private Map<UUID, String> createSnomeds( final List<UUID> componentUuidList) {
		final Map<UUID, String> result =  new HashMap<>();
		for (final UUID uuid : componentUuidList) {
			String hexString = Integer.toHexString(snomedIdCounter++);
			final int numberOfZeorsToPadd = 4 - hexString.length();
			if (hexString.length() < 4) {
				for (int j = 0; j < numberOfZeorsToPadd; j++) {
					hexString = "0" + hexString;
				}
			}
			result.put(uuid, SNOMED_ID_PREFIX + hexString);
		}
		return result;
	}

	@Override
	public String logIn(String userName, String passWord)
			throws RestClientException {
		return "token";
	}

	@Override
	public Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids, SchemeIdType schemeType, String comment) throws RestClientException {
		if (SchemeIdType.CTV3ID == schemeType) {
			return createCtv3Ids(uuids);
		} else if (SchemeIdType.SNOMEDID == schemeType) {
			return createSnomeds(uuids);
		} else {
			throw NOT_IMPLEMENTED_EXCEPTION;
		}
	}


}
