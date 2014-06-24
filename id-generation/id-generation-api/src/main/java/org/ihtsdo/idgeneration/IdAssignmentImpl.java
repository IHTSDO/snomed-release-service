package org.ihtsdo.idgeneration;

import org.apache.axis2.AxisFault;
import org.ihtsdo.idgen.ws.*;
import org.ihtsdo.idgen.ws.data.*;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class IdAssignmentImpl implements IdAssignmentBI {

	private Id_generatorStub idGenStub;

	public IdAssignmentImpl(String targetEndpoint) throws AxisFault {
		this.idGenStub = new Id_generatorStub(targetEndpoint);
	}

	@Override
	public Long getSCTID(UUID componentUuid) throws GetSCTIDFault, RemoteException {
		SCTIDRequest request = new SCTIDRequest();
		request.setComponentUuid(componentUuid.toString());

		SCTIDResponse sctid = idGenStub.getSCTID(request);

		BigInteger result = sctid.getSctid();
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		if (result != null) {
			return result.longValue();
		} else {
			return null;
		}
	}

	@Override
	public String getSNOMEDID(UUID componentUuid) throws RemoteException, GetSNOMEDIDFault {
		SNOMEDIDRequest request = new SNOMEDIDRequest();
		request.setComponentUuid(componentUuid.toString());

		SNOMEDIDResponse resonse = idGenStub.getSNOMEDID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return resonse.getSnomedid();
	}

	@Override
	public String getCTV3ID(UUID componentUuid) throws GetCTV3IDFault, RemoteException {
		CTV3IDRequest request = new CTV3IDRequest();
		request.setComponentUuid(componentUuid.toString());

		CTV3IDResponse ctv3id = idGenStub.getCTV3ID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return ctv3id.getCtv3Id();
	}

	@Override
	public HashMap<UUID, Long> getSCTIDList(List<UUID> componentUuidList) throws RemoteException, GetSCTIDListFault {
		long soTimeout = 3 * 60 * 1000;
		idGenStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(soTimeout);
		SCTIDListRequest request = new SCTIDListRequest();
		String[] componentUuidArray = new String[componentUuidList.size()];
		int i = 0;
		for (UUID componentUuid : componentUuidList) {
			componentUuidArray[i] = componentUuid.toString();
		}

		request.setComponentUuidList(componentUuidArray);

		SCTIDListResponse sctidList = idGenStub.getSCTIDList(request);
		CodeSctIdType[] sctidListArray = sctidList.getSctidList();
		HashMap<UUID, Long> result = new HashMap<UUID, Long>();
		for (CodeSctIdType codeSctIdType : sctidListArray) {
			BigInteger sctid = codeSctIdType.getSctid();
			System.out.println("[componentUUID: " + codeSctIdType.getCode() + " SCTID " + sctid + "]");
			result.put(UUID.fromString(codeSctIdType.getCode()), sctid.longValue());
		}
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return result;
	}

	@Override
	public Long createSCTID(UUID componentUuid, Integer namespaceId, String partitionId, String releaseId,
							String executionId, String moduleId) throws CreateSCTIDFaultException, RemoteException {
		CreateSCTIDRequest request = new CreateSCTIDRequest();
		request.setComponentUuid(componentUuid.toString());
		request.setExecutionId(executionId);
		request.setModuleId(moduleId);
		request.setNamespaceId(BigInteger.valueOf(namespaceId));
		request.setPartitionId(partitionId);
		request.setReleaseId(releaseId);

		CreateSCTIDResponse response = idGenStub.createSCTID(request);

		BigInteger sctid = response.getSctId();
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		if (sctid != null) {
			return sctid.longValue();
		} else {
			return null;
		}
	}

	@Override
	public String createSNOMEDID(UUID componentUuid, String parentSnomedId) throws RemoteException, CreateSNOMEDIDFaultException {
		CreateSNOMEDIDRequest request = new CreateSNOMEDIDRequest();

		request.setComponentUuid(componentUuid.toString());
		request.setParentSnomedId(parentSnomedId);

		CreateSNOMEDIDResponse response = idGenStub.createSNOMEDID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return response.getSnomedId();
	}

	@Override
	public String createCTV3ID(UUID componentUuid) throws RemoteException, CreateCTV3IDFaultException {
		CreateCTV3IDRequest request = new CreateCTV3IDRequest();
		request.setComponentUuid(componentUuid.toString());
		CreateCTV3IDResponse response = idGenStub.createCTV3ID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return response.getCtv3Id();
	}

	@Override
	public HashMap<UUID, Long> createSCTIDList(List<UUID> componentUuidList, Integer namespaceId, String partitionId,
											   String releaseId, String executionId, String moduleId)
			throws RemoteException, CreateSCTIDListFaultException {

		long soTimeout = 10 * 60 * 1000;
		idGenStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(soTimeout);

		CreateSCTIDListRequest request = new CreateSCTIDListRequest();

		String[] componentUuidArray = new String[componentUuidList.size()];
		int i = 0;
		for (UUID componentUuid : componentUuidList) {
			componentUuidArray[i] = componentUuid.toString();
			i++;
		}
		request.setComponentUuidList(componentUuidArray);
		request.setExecutionId(executionId);
		request.setModuleId(moduleId);
		request.setNamespaceId(BigInteger.valueOf(namespaceId));
		request.setPartitionId(partitionId);
		request.setReleaseId(releaseId);

		CreateSCTIDListResponse response = idGenStub.createSCTIDList(request);
		CodeSctIdType[] sctIdList = response.getSctidList();
		HashMap<UUID, Long> result = new HashMap<UUID, Long>();
		for (CodeSctIdType codeSctIdType : sctIdList) {
			BigInteger sctid = codeSctIdType.getSctid();
			result.put(UUID.fromString(codeSctIdType.getCode()), sctid.longValue());
		}
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();

		return result;
	}

	@Override
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<UUID, String> componentUuidParentSnoId,
																		  Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws RemoteException, CreateConceptIDListFaultException {
		CreateConceptIDListRequest request = new CreateConceptIDListRequest();

		CodeParentSnomedIdType[] componentUuidList = new CodeParentSnomedIdType[componentUuidParentSnoId.size()];
		int i = 0;
		for (UUID componentUuid : componentUuidParentSnoId.keySet()) {
			CodeParentSnomedIdType codeParentsid = new CodeParentSnomedIdType();
			codeParentsid.setCode(componentUuid.toString());
			codeParentsid.setParentSnomedId(componentUuidParentSnoId.get(componentUuid));
			componentUuidList[i] = codeParentsid;
			i++;
		}
		request.setComponentUuidList(componentUuidList);
		request.setExecutionId(executionId);
		request.setModuleId(moduleId);
		request.setNamespaceId(BigInteger.valueOf(namespaceId));
		request.setPartitionId(partitionId);
		request.setReleaseId(releaseId);

		CreateConceptIDListResponse response = idGenStub.createConceptIDList(request);

		ConceptIds[] conceptIds = response.getIdentifierConceptId();
		HashMap<UUID, HashMap<IDENTIFIER, String>> result = new HashMap<UUID, HashMap<IDENTIFIER, String>>();
		for (ConceptIds conceptIds2 : conceptIds) {
			IdentifierConceptId[] identifierIds = conceptIds2.getIdentifierConceptId();
			HashMap<IDENTIFIER, String> identifierId = new HashMap<IDENTIFIER, String>();
			for (IdentifierConceptId identifierConceptId : identifierIds) {
				IDENTIFIER[] identifiers = IDENTIFIER.values();
				for (IDENTIFIER identifier : identifiers) {
					if (identifier.getIdNumber() == identifierConceptId.getIdentifier().intValue()) {
						identifierId.put(identifier, identifierConceptId.getConceptId());
					}
				}
			}
			result.put(UUID.fromString(conceptIds2.getComponentUuid()), identifierId);
		}
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return result;
	}

	@Override
	public HashMap<IDENTIFIER, String> createConceptIds(UUID componentUuid, String parentSnomedId, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws RemoteException, CreateConceptIdsFaultException {
		CreateConceptIdsRequest request = new CreateConceptIdsRequest();
		request.setComponentUuid(componentUuid.toString());
		request.setParentSnomedId(parentSnomedId);
		request.setExecutionId(executionId);
		request.setModuleId(moduleId);
		request.setNamespaceId(BigInteger.valueOf(namespaceId));
		request.setPartitionId(partitionId);
		request.setReleaseId(releaseId);

		CreateConceptIdsResponse response = idGenStub.createConceptIds(request);
		IDString[] conceptIds = response.getConceptIds();
		HashMap<IDENTIFIER, String> result = new HashMap<IDENTIFIER, String>();
		for (IDString idString : conceptIds) {
			for (IDENTIFIER identifier : IDENTIFIER.values()) {
				if (identifier.getIdNumber() == idString.getIdentifier().intValue()) {
					result.put(identifier, idString.getId());
					System.out.println("[IDENTIFIER: " + idString.getIdentifier() + " ID: " + idString.getId() + "]");
				}
			}
		}
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return result;
	}

}
