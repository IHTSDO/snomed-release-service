package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
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

	public Long getSCTIDFromCache(String uuidString) {
		return uuidToSctidCache.get(uuidString);
	}

}
