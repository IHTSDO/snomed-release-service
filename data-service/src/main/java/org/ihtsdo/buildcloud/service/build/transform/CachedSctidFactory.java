package org.ihtsdo.buildcloud.service.build.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedSctidFactory {

	public static final Logger LOGGER = LoggerFactory.getLogger(CachedSctidFactory.class);
	private final Integer namespaceId;
	private final IdServiceRestClient idServiceRestClient;
	private final Map<String, Long> uuidToSctidCache;
	private String comment;

	public CachedSctidFactory(final Integer namespaceId, final String releaseId, final String buildId, final IdServiceRestClient idRestClient) {
		this.namespaceId = namespaceId;
		this.idServiceRestClient = idRestClient;
		comment = "ReleaseId:" + releaseId + " BuildId:" + buildId;
		uuidToSctidCache = new ConcurrentHashMap<>();
	}

	public Long getSCTID(final String componentUuid, final String partitionId, final String moduleId) throws Exception{
		if (!uuidToSctidCache.containsKey(componentUuid)) {
			Long sctid = idServiceRestClient.getOrCreateSctId(UUID.fromString(componentUuid), namespaceId, partitionId, comment);
			if ( sctid != null) {
				uuidToSctidCache.put(componentUuid, sctid);
			}
		}
		return uuidToSctidCache.get(componentUuid);
	}

	public Map<String, Long> getSCTIDs(final List<String> componentUuidStrings, final String partitionId, final String moduleId) throws RestClientException {
		final Map<String, Long> uuidStringToSctidMapResults = new HashMap<>();
		if (componentUuidStrings == null || componentUuidStrings.isEmpty()) {
			return uuidStringToSctidMapResults;
		}
		Map<UUID, Long> uuidToSctidMap =  new HashMap<>();
		// Convert uuid strings to UUID objects
		final List<UUID> componentUuids = new ArrayList<>();
		for (final String componentUuidString : componentUuidStrings) {
			componentUuids.add(UUID.fromString(componentUuidString));
		}
		LOGGER.info("Batch ID service request, batch size {}.", componentUuids.size());
		if ( !componentUuids.isEmpty()) {
			uuidToSctidMap = idServiceRestClient.getOrCreateSctIds(componentUuids, namespaceId, partitionId, comment);
		}
		
		// Store results in cache
		for (final UUID uuid : uuidToSctidMap.keySet()) {
			final String uuidString = uuid.toString();
			final Long value = uuidToSctidMap.get(uuid);
			uuidToSctidCache.put(uuidString, value);
			uuidStringToSctidMapResults.put(uuidString, value);
		}
		return uuidStringToSctidMapResults;
	}

	public Long getSCTIDFromCache(final String uuidString) {
		return uuidToSctidCache.get(uuidString);
	}

}
