package org.ihtsdo.buildcloud.service.build.transform;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDListFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedSctidFactory {

	public static final Logger LOGGER = LoggerFactory.getLogger(CachedSctidFactory.class);
	private final Integer namespaceId;
	private final String releaseId;
	private final String buildId;
	private final IdAssignmentBI idAssignment;
	private final Map<String, Long> uuidToSctidCache;
	private final int maxTries;
	private final int retryDelaySeconds;

	public CachedSctidFactory(final Integer namespaceId, final String releaseId, final String buildId, final IdAssignmentBI idAssignment, final int maxTries, final int retryDelaySeconds) {
		this.namespaceId = namespaceId;
		this.releaseId = releaseId;
		this.buildId = buildId;
		this.idAssignment = idAssignment;
		uuidToSctidCache = new ConcurrentHashMap<>();
		this.maxTries = maxTries;
		this.retryDelaySeconds = retryDelaySeconds;
	}

	public Long getSCTID(final String componentUuid, final String partitionId, final String moduleId) throws CreateSCTIDFaultException, RemoteException, InterruptedException{
		if (!uuidToSctidCache.containsKey(componentUuid)) {
			Long sctid = null;
			int attempt = 1;
			while (sctid == null) {
				try {
					sctid = idAssignment.createSCTID(UUID.fromString(componentUuid), namespaceId, partitionId, releaseId, buildId, moduleId);
				} catch (CreateSCTIDFaultException | RemoteException e) {
					if (attempt < maxTries) {
						LOGGER.warn("ID Gen lookup failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
						attempt++;
						Thread.sleep(retryDelaySeconds * 1000);
					} else {
						throw e;
					}
				}
			}
			uuidToSctidCache.put(componentUuid, sctid);
		}
		return uuidToSctidCache.get(componentUuid);
	}

	public Map<String, Long> getSCTIDs(final List<String> componentUuidStrings, final String partitionId, final String moduleId) throws CreateSCTIDListFaultException, RemoteException, InterruptedException {
		final Map<String, Long> uuidStringToSctidMapResults = new HashMap<>();
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
				LOGGER.info("Batch ID Gen lookup, batch size {}.", componentUuids.size());
				uuidToSctidMap = idAssignment.createSCTIDList(componentUuids, namespaceId, partitionId, releaseId, buildId, moduleId);
			} catch (CreateSCTIDListFaultException | RemoteException e) {
				if (attempt < maxTries) {
					LOGGER.warn("Batch ID Gen lookup failed on attempt {}. Waiting {} seconds before retrying.", attempt, retryDelaySeconds, e);
					attempt++;
					Thread.sleep(retryDelaySeconds * 1000);
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
