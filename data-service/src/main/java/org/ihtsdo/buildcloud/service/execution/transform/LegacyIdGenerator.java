package org.ihtsdo.buildcloud.service.execution.transform;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.buildcloud.service.execution.transform.TopologicalSort.DirectedGraph;
import org.ihtsdo.idgen.ws.CreateCTV3IDListFaultException;
import org.ihtsdo.idgen.ws.CreateSnomedIDListFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;

public class LegacyIdGenerator {
	private final IdAssignmentBI idAssignment;
	public LegacyIdGenerator(final IdAssignmentBI idAssignmentBI) {
		idAssignment = idAssignmentBI;
	}

	public Map<UUID, String> generateCTV3IDs(final List<UUID> newConceptUuids) throws TransformationException {
		try {
			return idAssignment.createCTV3IDList(newConceptUuids);
		} catch (RemoteException | CreateCTV3IDListFaultException e) {
			throw new TransformationException("Failed to generate CTV3 IDs", e);
		}
	}

	public Map<Long, String> generateSnomedIds(final Map<Long, Long> sctIdAndParentMap) throws TransformationException {
		try {
			final DirectedGraph<Long> graph = new DirectedGraph<>();
			for (final Long sctId : sctIdAndParentMap.keySet()) {
				graph.addNode(sctId);
				graph.addNode(sctIdAndParentMap.get(sctId));
				graph.addEdge(sctIdAndParentMap.get(sctId), sctId);
			}
				//sorting them to topological order
				final List<Long> result = TopologicalSort.sort(graph);
				final Long[][] sctIdAndParentSctIdArray = new Long[sctIdAndParentMap.keySet().size()][2];
				int i = 0;
				for ( final Long sctId : result) {
					if ( sctIdAndParentMap.containsKey(sctId)) {
						sctIdAndParentSctIdArray[i][0] = sctId;
						sctIdAndParentSctIdArray[i][1] = sctIdAndParentMap.get(sctId);
						i++;
					}
				}
				return idAssignment.createSNOMEDIDList(sctIdAndParentSctIdArray);
			
		} catch (final RemoteException | CreateSnomedIDListFaultException  e) {
			throw new TransformationException("Failed to generate Snomed IDs", e);
		}
	}

}
