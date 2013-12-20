package org.ihtsdo.idgeneration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.axis2.AxisFault;
import org.ihtsdo.idgen.ws.Id_generatorStub;
import org.ihtsdo.idgen.ws.data.CTV3IDRequest;
import org.ihtsdo.idgen.ws.data.CTV3IDResponse;
import org.ihtsdo.idgen.ws.data.CodeParentSnomedIdType;
import org.ihtsdo.idgen.ws.data.CodeSctIdType;
import org.ihtsdo.idgen.ws.data.ConceptIds;
import org.ihtsdo.idgen.ws.data.CreateCTV3IDRequest;
import org.ihtsdo.idgen.ws.data.CreateCTV3IDResponse;
import org.ihtsdo.idgen.ws.data.CreateConceptIDListRequest;
import org.ihtsdo.idgen.ws.data.CreateConceptIDListResponse;
import org.ihtsdo.idgen.ws.data.CreateConceptIdsRequest;
import org.ihtsdo.idgen.ws.data.CreateConceptIdsResponse;
import org.ihtsdo.idgen.ws.data.CreateSCTIDListRequest;
import org.ihtsdo.idgen.ws.data.CreateSCTIDListResponse;
import org.ihtsdo.idgen.ws.data.CreateSCTIDRequest;
import org.ihtsdo.idgen.ws.data.CreateSCTIDResponse;
import org.ihtsdo.idgen.ws.data.CreateSNOMEDIDRequest;
import org.ihtsdo.idgen.ws.data.CreateSNOMEDIDResponse;
import org.ihtsdo.idgen.ws.data.IDString;
import org.ihtsdo.idgen.ws.data.IdentifierConceptId;
import org.ihtsdo.idgen.ws.data.SCTIDListRequest;
import org.ihtsdo.idgen.ws.data.SCTIDListResponse;
import org.ihtsdo.idgen.ws.data.SCTIDRequest;
import org.ihtsdo.idgen.ws.data.SCTIDResponse;
import org.ihtsdo.idgen.ws.data.SNOMEDIDRequest;
import org.ihtsdo.idgen.ws.data.SNOMEDIDResponse;

public class IdAssignmentImpl implements IdAssignmentBI {

	private String targetEndpoint = "http://mgr.servers.aceworkspace.net:50002/axis2/services/id_generator";
	private Id_generatorStub idGenStub;
	private static final String WEB_SERVICE_IMPL = "WebServiceImplementation";

	public IdAssignmentImpl() {
		super();
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("config/webservice.properties"));
			this.targetEndpoint = props.getProperty("targetEndpoint");
			this.idGenStub = new Id_generatorStub(targetEndpoint);
		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public IdAssignmentImpl(File propertiesFile) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("config/webservice.properties"));
			this.targetEndpoint = props.getProperty("targetEndpoint");
			this.idGenStub = new Id_generatorStub(targetEndpoint);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public IdAssignmentImpl(String targetEndpoint, String user, String password) {
		super();
		this.targetEndpoint = targetEndpoint;
		try {
			this.idGenStub = new Id_generatorStub(targetEndpoint);
		} catch (AxisFault e) {
			e.printStackTrace();
		}
	}

	@Override
	public Long getSCTID(UUID componentUuid) throws Exception {
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
	public String getSNOMEDID(UUID componentUuid) throws Exception {
		SNOMEDIDRequest request = new SNOMEDIDRequest();
		request.setComponentUuid(componentUuid.toString());

		SNOMEDIDResponse resonse = idGenStub.getSNOMEDID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return resonse.getSnomedid();
	}

	@Override
	public String getCTV3ID(UUID componentUuid) throws Exception {
		CTV3IDRequest request = new CTV3IDRequest();
		request.setComponentUuid(componentUuid.toString());

		CTV3IDResponse ctv3id = idGenStub.getCTV3ID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return ctv3id.getCtv3Id();
	}

	@Override
	public HashMap<UUID, Long> getSCTIDList(List<UUID> componentUuidList) throws Exception {
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
	public Long createSCTID(UUID componentUuid, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws Exception {
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
	public String createSNOMEDID(UUID componentUuid, String parentSnomedId) throws Exception {
		CreateSNOMEDIDRequest request = new CreateSNOMEDIDRequest();

		request.setComponentUuid(componentUuid.toString());
		request.setParentSnomedId(parentSnomedId);

		CreateSNOMEDIDResponse response = idGenStub.createSNOMEDID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return response.getSnomedId();
	}

	@Override
	public String createCTV3ID(UUID componentUuid) throws Exception {
		CreateCTV3IDRequest request = new CreateCTV3IDRequest();
		request.setComponentUuid(componentUuid.toString());
		CreateCTV3IDResponse response = idGenStub.createCTV3ID(request);
		idGenStub._getServiceClient().cleanupTransport();
		idGenStub._getServiceClient().cleanup();
		return response.getCtv3Id();
	}

	@Override
	public HashMap<UUID, Long> createSCTIDList(List<UUID> componentUuidList, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws Exception {
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
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<UUID, String> componentUuidParentSnoId, Integer namespaceId, String partitionId, String releaseId,
			String executionId, String moduleId) throws Exception {
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
	public HashMap<IDENTIFIER, String> createConceptIds(UUID componentUuid, String parentSnomedId, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId)
			throws Exception {
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
