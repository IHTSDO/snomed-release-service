package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SCTIDTransformation implements LineTransformation {

	private IdAssignmentBI idAssignment;
	private final int componentIdCol;
	private final int moduleIdCol;
	private final int namespaceId;
	private final String partitionId;
	private final String releaseId;
	private final String executionId;

	private final Map<String, String> cache;

	public SCTIDTransformation(IdAssignmentBI idAssignment, int componentIdCol, int moduleIdCol, int namespaceId, String partitionId,
							   String releaseId, String executionId) {
		this.idAssignment = idAssignment;
		this.componentIdCol = componentIdCol;
		this.moduleIdCol = moduleIdCol;
		this.namespaceId = namespaceId;
		this.partitionId = partitionId;
		this.releaseId = releaseId; // effectiveTime
		this.executionId = executionId; // use ours

		this.cache = new HashMap();
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues[componentIdCol].contains("-")) {
			String uuidString = columnValues[componentIdCol];
			if (!cache.containsKey(uuidString)) {
				UUID uuid = UUID.fromString(uuidString);
				String moduleId = columnValues[moduleIdCol];
				try {
					Long sctId = idAssignment.createSCTID(uuid, namespaceId, partitionId, releaseId, executionId, moduleId);
					String sctIdString = sctId.toString();
					cache.put(uuidString, sctIdString);
				} catch (CreateSCTIDFaultException | RemoteException e) {
					throw new TransformationException("SCTID creation request failed.", e);
				}
			}
			columnValues[componentIdCol] = cache.get(uuidString);
		}
	}

}
