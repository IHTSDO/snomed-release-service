package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDListFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CachedSctidFactory {

	public static final Logger LOGGER = LoggerFactory.getLogger(CachedSctidFactory.class);
	private Integer namespaceId;
	private String releaseId;
	private String executionId;
	private IdAssignmentBI idAssignment;
	private Map<String, Long> uuidToSctidCache;
	private final int maxTries;
	private final int retryDelaySeconds;

	public CachedSctidFactory(Integer namespaceId, String releaseId, String executionId, IdAssignmentBI idAssignment, int maxTries, int retryDelaySeconds) {
		this.namespaceId = namespaceId;
		this.releaseId = releaseId;
		this.executionId = executionId;
		this.idAssignment = idAssignment;
		uuidToSctidCache = new ConcurrentHashMap<>();
		this.maxTries = maxTries;
		this.retryDelaySeconds = retryDelaySeconds;
	}

	public Long getSCTID(String componentUuid, String partitionId, String moduleId) throws org.ihtsdo.idgen.ws.CreateSCTIDFaultException, java.rmi.RemoteException, InterruptedException {
		if (!uuidToSctidCache.containsKey(componentUuid)) {
			Long sctid = null;
			int attempt = 1;
			while (sctid == null) {
				try {
					sctid = idAssignment.createSCTID(UUID.fromString(componentUuid), namespaceId, partitionId, releaseId, executionId, moduleId);
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

	public Map<String, Long> getSCTIDs(List<String> componentUuidStrings, String partitionId, String moduleId) throws RemoteException, CreateSCTIDListFaultException, InterruptedException {
		Map<String, Long> uuidStringToSctidMapResults = new HashMap<>();
		Map<UUID, Long> uuidToSctidMap = null;

		// Convert uuid strings to UUID objects
		List<UUID> componentUuids = new ArrayList<>();
		for (String componentUuidString : componentUuidStrings) {
			componentUuids.add(UUID.fromString(componentUuidString));
		}

		// Lookup with retries
		int attempt = 1;
		while (uuidToSctidMap == null) {
			try {
				LOGGER.info("Batch ID Gen lookup, batch size {}.", componentUuids.size());
				uuidToSctidMap = idAssignment.createSCTIDList(componentUuids, namespaceId, partitionId, releaseId, executionId, moduleId);
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
		for (UUID uuid : uuidToSctidMap.keySet()) {
			String uuidString = uuid.toString();
			Long value = uuidToSctidMap.get(uuid);
			uuidToSctidCache.put(uuidString, value);
			uuidStringToSctidMapResults.put(uuidString, value);
		}

		return uuidStringToSctidMapResults;
	}

	public Long getSCTIDFromCache(String uuidString) {
		return uuidToSctidCache.get(uuidString);
	}

}
