package org.ihtsdo.idgen.ws;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
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

import com.termmed.genid.SctIdGen.IDENTIFIER;
import com.termmed.genid.SctIdGenImpl;

public class Id_generatorSkeleton implements Id_generatorSkeletonInterface {
	
	private static final Logger logger = Logger.getLogger(Id_generatorSkeleton.class);

	public CreateCTV3IDResponse createCTV3ID(CreateCTV3IDRequest request) throws CreateCTV3IDFaultException {
		SctIdGenImpl idgen = new SctIdGenImpl();
		CreateCTV3IDResponse result = new CreateCTV3IDResponse();
		String ctv3Id = null;
		try {
			ctv3Id = idgen.createCTV3ID(request.getComponentUuid());
			if(ctv3Id == null){
				throw new CreateCTV3IDFaultException("There is no concept with " +request.getComponentUuid() + " uuid to create ctv3id for");
			}
		} catch (Exception e) {
			idgen.rollbackSession();
			throw new CreateCTV3IDFaultException("Execption while trying to create CTV3ID: " + e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
		}
		result.setCtv3Id(ctv3Id);
		return result;
	}

	public CreateSCTIDListResponse createSCTIDList(CreateSCTIDListRequest request) throws CreateSCTIDListFaultException {
		CreateSCTIDListResponse response = new CreateSCTIDListResponse();
		CodeSctIdType[] sctIdList = null;

		SctIdGenImpl idgen = new SctIdGenImpl();

		String[] componentUuidListReq = request.getComponentUuidList();
		List<String> componentUuidList = new ArrayList<String>();
		for (String componentUuid : componentUuidListReq) {
			componentUuidList.add(componentUuid);
		}

		String moduleId = request.getModuleId();
		String executionId = request.getExecutionId();
		String partitionId = request.getPartitionId();
		String releaseId = request.getReleaseId();
		Integer namespaceId = request.getNamespaceId().intValue();

		try {
			HashMap<String, Long> result = idgen.createSCTIDList(componentUuidList, namespaceId, partitionId, releaseId, executionId, moduleId);
			sctIdList = new CodeSctIdType[result.size()];
			int i = 0;
			for (String componentUuid : result.keySet()) {
				CodeSctIdType currentCodeSctId = new CodeSctIdType();
				currentCodeSctId.setCode(componentUuid);
				Long sctid = result.get(componentUuid);
				if(sctid != null){
					currentCodeSctId.setSctid(BigInteger.valueOf(sctid));
				}else{
					currentCodeSctId.setSctid(null);
				}
					
				sctIdList[i] = currentCodeSctId;
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			idgen.rollbackSession();
			throw new CreateSCTIDListFaultException("Exeption trying to create SCTIDList: ", e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
			System.gc();
		}

		response.setSctidList(sctIdList);
		return response;
	}

	public CreateSNOMEDIDResponse createSNOMEDID(CreateSNOMEDIDRequest request) throws CreateSNOMEDIDFaultException {
		CreateSNOMEDIDResponse response = new CreateSNOMEDIDResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();
		try {
			response.setSnomedId(idgen.createSNOMEDID(request.getComponentUuid(), request.getParentSnomedId()));
		} catch (Exception e) {
			e.printStackTrace();
			idgen.rollbackSession();
			throw new CreateSNOMEDIDFaultException("Exeption trying to create SNOMEDID: ", e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
		}

		return response;
	}

	public CreateConceptIdsResponse createConceptIds(CreateConceptIdsRequest request) throws CreateConceptIdsFaultException {
		CreateConceptIdsResponse response = new CreateConceptIdsResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();

		try {
			HashMap<IDENTIFIER, String> result = idgen.createConceptIds(request.getComponentUuid(), request.getParentSnomedId(), request.getNamespaceId().intValue(), request.getPartitionId(), request.getReleaseId(),
					request.getExecutionId(), request.getModuleId());
			
			IDString[] idStringArray = new IDString[result.size()];
			int i = 0;
			for (IDENTIFIER id : result.keySet()) {
				IDString currentIdString = new IDString();
				currentIdString.setId(result.get(id));
				currentIdString.setIdentifier(BigInteger.valueOf(id.getIdNumber()));
				idStringArray[i] = currentIdString;
				i++;
			}
			response.setConceptIds(idStringArray);
		} catch (Exception e) {
			e.printStackTrace();
			idgen.rollbackSession();
			throw new CreateConceptIdsFaultException("Exeption trying to create ConceptIds", e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
		}
		return response;
	}

	public SCTIDListResponse getSCTIDList(SCTIDListRequest request) throws GetSCTIDListFault {
		SCTIDListResponse response = new SCTIDListResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();

		try {
			List<String> componentUuidList = new ArrayList<String>();
			String[] componentUuidArray = request.getComponentUuidList();
			for (String componentUuid : componentUuidArray) {
				componentUuidList.add(componentUuid);
			}
			HashMap<String, Long> sctIdList = idgen.getSCTIDList(componentUuidList);
			CodeSctIdType[] sctIdArray = new CodeSctIdType[sctIdList.size()];
			int i = 0;
			for (String loopComponentUuid : sctIdList.keySet()) {
				CodeSctIdType sctIdType = new CodeSctIdType();
				sctIdType.setCode(loopComponentUuid);
				Long sctid = sctIdList.get(loopComponentUuid);
				if(sctid != null){
					sctIdType.setSctid(BigInteger.valueOf(sctid));
				}else{
					sctIdType.setSctid(null);
				}
				sctIdArray[i] = sctIdType;
				i++;
			}
			response.setSctidList(sctIdArray);
		} catch (Exception e) {
			e.printStackTrace();
			throw new GetSCTIDListFault("Exception trying to get SCTID List: ", e);
		} finally {
			idgen.closeSession();
		}

		return response;
	}

	public CreateSCTIDResponse createSCTID(CreateSCTIDRequest request) throws CreateSCTIDFaultException {
		CreateSCTIDResponse response = new CreateSCTIDResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();
		
		Long sctid = null;
		try {
			sctid = idgen.createSCTID(request.getComponentUuid(), request.getNamespaceId().intValue(), request.getPartitionId().toString(),
					request.getReleaseId().toString(), request.getExecutionId(), request.getModuleId().toString());
		} catch (Exception e) {
			idgen.rollbackSession();
			e.printStackTrace();
			throw new CreateSCTIDFaultException("Exception trying to create SCTID: ", e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
		}
		if(sctid != null){
			response.setSctId(BigInteger.valueOf(sctid));
		}else{
			response.setSctId(null);
		}
		return response;
	}

	public SCTIDResponse getSCTID(SCTIDRequest request) throws GetSCTIDFault {
		SCTIDResponse response = new SCTIDResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();
		try {
			String componentUuid = request.getComponentUuid();
			logger.debug("COMPONENT UUID: " + componentUuid);
			Long sctid = idgen.getSCTID(componentUuid);
			logger.debug("SCTID: " + sctid);
			if(sctid != null){
				response.setSctid(BigInteger.valueOf(sctid));
			}else{
				response.setSctid(null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GetSCTIDFault("Exception trying to get SCTID: ", e);
		} finally {
			idgen.closeSession();
		}
		return response;
	}

	public SNOMEDIDResponse getSNOMEDID(SNOMEDIDRequest request) throws GetSNOMEDIDFault {
		SNOMEDIDResponse response = new SNOMEDIDResponse();
		SctIdGenImpl idgen = new SctIdGenImpl();
		
		try {
			response.setSnomedid(idgen.getSNOMEDID(request.getComponentUuid()));
		} catch (Exception e) {
			e.printStackTrace();
			throw new GetSNOMEDIDFault("Exception trying to get SNOMEDID: ", e);
		} finally {
			idgen.closeSession();
		}
		
		return response;
	}

	public CTV3IDResponse getCTV3ID(CTV3IDRequest request) throws GetCTV3IDFault {
		SctIdGenImpl idgen = new SctIdGenImpl();
		CTV3IDResponse response = new CTV3IDResponse();
		
		try {
			response.setCtv3Id(idgen.getCTV3ID(request.getComponentUuid()));
		} catch (Exception e) {
			e.printStackTrace();
			throw new GetCTV3IDFault("Exception trying to get CTV3ID: ", e);
		} finally {
			idgen.closeSession();
		}
		
		return response;
	}

	public CreateConceptIDListResponse createConceptIDList(CreateConceptIDListRequest request) throws CreateConceptIDListFaultException {
		SctIdGenImpl idgen = new SctIdGenImpl();
		CreateConceptIDListResponse response = new CreateConceptIDListResponse();
		try {
			HashMap<String, String> componentUUIDandParentSnomedId = new HashMap<String, String>();
			CodeParentSnomedIdType[] compUuidAndParentSnomedIdArray = request.getComponentUuidList();
			for (CodeParentSnomedIdType uuidParentId : compUuidAndParentSnomedIdArray) {
				componentUUIDandParentSnomedId.put(uuidParentId.getCode(), uuidParentId.getParentSnomedId());
			}
			Integer namespaceId = request.getNamespaceId().intValue();
			String partitionId = request.getPartitionId();
			String releaseId = request.getReleaseId();
			String executionId = request.getExecutionId();
			String moduleId = request.getModuleId();
			
			HashMap<String, HashMap<IDENTIFIER, String>> result = idgen.createConceptIDList(componentUUIDandParentSnomedId, namespaceId, partitionId, releaseId, executionId, moduleId);
			
			ConceptIds[] conceptIds = new ConceptIds[result.size()];
			int i = 0;
			for (String componentUuid : result.keySet()) {
				ConceptIds conceptId = new ConceptIds();
				conceptId.setComponentUuid(componentUuid);
				
				HashMap<IDENTIFIER, String> identifierConceptIdMap = result.get(componentUuid);
				IdentifierConceptId[] identifierConceptIds = new IdentifierConceptId[identifierConceptIdMap.size()];
				conceptId.setIdentifierConceptId(identifierConceptIds);
				int j = 0;
				for (IDENTIFIER identifier : identifierConceptIdMap.keySet()) {
					IdentifierConceptId identifierConceptId = new IdentifierConceptId();
					identifierConceptId.setIdentifier(BigInteger.valueOf(identifier.getIdNumber()));
					identifierConceptId.setConceptId(identifierConceptIdMap.get(identifier));
					identifierConceptIds[j] = identifierConceptId;
					j++;
				}
				conceptIds[i] = conceptId;
				i++;
			}
			response.setIdentifierConceptId(conceptIds);
		} catch (Exception e) {
			idgen.rollbackSession();
			e.printStackTrace();
			throw new CreateConceptIDListFaultException("Exception trying to create Concept ID List: ", e);
		} finally {
			idgen.commitSession();
			idgen.closeSession();
		}
		return response;
	}

}
