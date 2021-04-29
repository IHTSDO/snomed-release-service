package org.ihtsdo.buildcloud.service.build.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedSctidFactory {

	public static final Logger LOGGER = LoggerFactory.getLogger(CachedSctidFactory.class);
	private final Integer namespaceId;
	private final IdServiceRestClient idServiceRestClient;
	private final Map<String, Long> uuidToSctidCache;
	private final String comment;
	private final int maxTries;
	private final int retryDelaySeconds;
	private Build build;
	private BuildDAO buildDAO;

	public CachedSctidFactory(final Integer namespaceId, final String releaseId, final String buildId, final IdServiceRestClient idRestClient, final int maxtries, final int retryDelayDeconds) {
		this.namespaceId = namespaceId;
		this.idServiceRestClient = idRestClient;
		comment = "ReleaseId:" + releaseId + " BuildId:" + buildId;
		uuidToSctidCache = new ConcurrentHashMap<>();
		this.maxTries = maxtries;
		this.retryDelaySeconds = retryDelayDeconds;
	}

	public CachedSctidFactory(final Integer namespaceId, final String releaseId, final Build build, final BuildDAO buildDAO, final IdServiceRestClient idRestClient, final int maxtries, final int retryDelayDeconds) {
		this.namespaceId = namespaceId;
		this.idServiceRestClient = idRestClient;
		comment = "ReleaseId:" + releaseId + " BuildId:" + build.getId();
		uuidToSctidCache = new ConcurrentHashMap<>();
		this.maxTries = maxtries;
		this.retryDelaySeconds = retryDelayDeconds;
		this.buildDAO = buildDAO;
		this.build = build;
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
		Map<UUID, Long> uuidToSctidMap = null;
		// Convert uuid strings to UUID objects
		final List<UUID> componentUuids = new ArrayList<>();
		for (final String componentUuidString : componentUuidStrings) {
			componentUuids.add(UUID.fromString(componentUuidString));
		}
		// Lookup with retries
		int attempt = 1;
		while (uuidToSctidMap == null) {
			try {
				if (build != null && buildDAO != null && buildDAO.isBuildCancelRequested(build)) {
					LOGGER.warn("Stop requesting ID from CIS. Build status has been changed to CANCEL_REQUESTED");
					throw new BusinessServiceRuntimeException("Stop requesting ID from CIS. Build status has been changed to CANCEL_REQUESTED");
				}
				LOGGER.info("Batch ID service request, batch size {}.", componentUuids.size());
				if ( !componentUuids.isEmpty()) {
					uuidToSctidMap = idServiceRestClient.getOrCreateSctIds(componentUuids, namespaceId, partitionId, comment);
				}
			} catch (RestClientException e) {
				if (attempt < maxTries) {
					LOGGER.warn("Batch ID service lookup failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
					attempt++;
					try {
						Thread.sleep(retryDelaySeconds * 1000);
					} catch (InterruptedException ie) {
						throw new RestClientException("id service retry interupted", ie);
					}
				} else {
					throw e;
				}
			}
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
