package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.idgeneration.IdAssignmentBI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CachedSctidFactory {

	private Integer namespaceId;
	private String releaseId;
	private String executionId;
	private IdAssignmentBI idAssignment;
	private Map<String, Long> uuidToSctidCache;

	public CachedSctidFactory(Integer namespaceId, String releaseId, String executionId, IdAssignmentBI idAssignment) {
		this.namespaceId = namespaceId;
		this.releaseId = releaseId;
		this.executionId = executionId;
		this.idAssignment = idAssignment;
		uuidToSctidCache = new ConcurrentHashMap<>();
	}

	public Long getSCTID(String componentUuid, String partitionId, String moduleId) throws org.ihtsdo.idgen.ws.CreateSCTIDFaultException, java.rmi.RemoteException {
		if (!uuidToSctidCache.containsKey(componentUuid)) {
			Long sctid = idAssignment.createSCTID(UUID.fromString(componentUuid), namespaceId, partitionId, releaseId, executionId, moduleId);
			uuidToSctidCache.put(componentUuid, sctid);
		}
		return uuidToSctidCache.get(componentUuid);
	}

	public Long getSCTIDFromCache(String uuidString) {
		return uuidToSctidCache.get(uuidString);
	}

}
