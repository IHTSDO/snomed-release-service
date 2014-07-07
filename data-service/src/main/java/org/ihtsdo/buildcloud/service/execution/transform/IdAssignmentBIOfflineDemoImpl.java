package org.ihtsdo.buildcloud.service.execution.transform;

import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.idgen.ws.*;
import org.ihtsdo.idgeneration.IdAssignmentBI;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class IdAssignmentBIOfflineDemoImpl implements IdAssignmentBI {

	private Long sctid;

	private static final String BOGUS_CHECK_DIGIT = "1";
	private static final NotImplementedException NOT_IMPLEMENTED_EXCEPTION = new NotImplementedException("Not implemented in the offline demo id generator.");

	public IdAssignmentBIOfflineDemoImpl() {
		sctid = 800000L;
	}

	@Override
	public Long createSCTID(UUID componentUuid, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws CreateSCTIDFaultException, RemoteException {
		sctid++;
		return Long.parseLong("" + sctid + partitionId + BOGUS_CHECK_DIGIT);
	}

	@Override
	public Long getSCTID(UUID componentUuid) throws GetSCTIDFault, RemoteException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String getSNOMEDID(UUID componentUuid) throws RemoteException, GetSNOMEDIDFault {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String getCTV3ID(UUID componentUuid) throws GetCTV3IDFault, RemoteException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<UUID, Long> getSCTIDList(List<UUID> componentUuidList) throws RemoteException, GetSCTIDListFault {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String createSNOMEDID(UUID componentUuid, String parentSnomedId) throws RemoteException, CreateSNOMEDIDFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String createCTV3ID(UUID componentUuid) throws RemoteException, CreateCTV3IDFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<IDENTIFIER, String> createConceptIds(UUID componentUuid, String parentSnomedId, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws RemoteException, CreateConceptIdsFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<UUID, Long> createSCTIDList(List<UUID> componentUuidList, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws RemoteException, CreateSCTIDListFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<UUID, String> componentUUIDandParentSnomedId, Integer namespaceId, String partitionId, String releaseId, String executionId, String moduleId) throws RemoteException, CreateConceptIDListFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}
}
