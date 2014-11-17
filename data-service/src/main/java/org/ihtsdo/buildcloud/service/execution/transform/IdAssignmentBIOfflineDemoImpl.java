package org.ihtsdo.buildcloud.service.execution.transform;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.idgen.ws.CreateCTV3IDFaultException;
import org.ihtsdo.idgen.ws.CreateCTV3IDListFaultException;
import org.ihtsdo.idgen.ws.CreateConceptIDListFaultException;
import org.ihtsdo.idgen.ws.CreateConceptIdsFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDFaultException;
import org.ihtsdo.idgen.ws.CreateSCTIDListFaultException;
import org.ihtsdo.idgen.ws.CreateSNOMEDIDFaultException;
import org.ihtsdo.idgen.ws.CreateSnomedIDListFaultException;
import org.ihtsdo.idgen.ws.GetCTV3IDFault;
import org.ihtsdo.idgen.ws.GetCTV3IDListFaultException;
import org.ihtsdo.idgen.ws.GetSCTIDFault;
import org.ihtsdo.idgen.ws.GetSCTIDListFault;
import org.ihtsdo.idgen.ws.GetSNOMEDIDFault;
import org.ihtsdo.idgen.ws.GetSnomedIDListFaultException;
import org.ihtsdo.idgeneration.IdAssignmentBI;

public class IdAssignmentBIOfflineDemoImpl implements IdAssignmentBI {
	//Only for test purpose
	private static final String CTV3ID_PREFIX = "XUsW";

	private static final String SNOMED_ID_PREFIX = "R-F";

	private Long sctid;

	private static final String BOGUS_CHECK_DIGIT = "1";
	private static final NotImplementedException NOT_IMPLEMENTED_EXCEPTION = new NotImplementedException("Not implemented in the offline demo id generator.");

	private int snomedIdCounter;
	private int ctv3IdChar;

	public IdAssignmentBIOfflineDemoImpl() {
		reset();
	}

	@Override
	public Long createSCTID(final UUID componentUuid, final Integer namespaceId, final String partitionId, final String releaseId, final String executionId, final String moduleId) throws CreateSCTIDFaultException, RemoteException {
		return createNewId(partitionId);
	}

	@Override
	public HashMap<UUID, Long> createSCTIDList(final List<UUID> componentUuidList, final Integer namespaceId, final String partitionId, final String releaseId, final String executionId, final String moduleId) throws RemoteException, CreateSCTIDListFaultException {
		final HashMap<UUID, Long> map = new HashMap<>();
		for (final UUID uuid : componentUuidList) {
			map.put(uuid, createNewId(partitionId));
		}
		return map;
	}

	public Long createNewId(final String partitionId) {
		sctid++;
		return Long.parseLong("" + sctid + partitionId + BOGUS_CHECK_DIGIT);
	}

	@Override
	public Long getSCTID(final UUID componentUuid) throws GetSCTIDFault, RemoteException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String getSNOMEDID(final UUID componentUuid) throws RemoteException, GetSNOMEDIDFault {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String getCTV3ID(final UUID componentUuid) throws GetCTV3IDFault, RemoteException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<UUID, Long> getSCTIDList(final List<UUID> componentUuidList) throws RemoteException, GetSCTIDListFault {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String createSNOMEDID(final UUID componentUuid, final String parentSnomedId) throws RemoteException, CreateSNOMEDIDFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public String createCTV3ID(final UUID componentUuid) throws RemoteException, CreateCTV3IDFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<IDENTIFIER, String> createConceptIds(final UUID componentUuid, final String parentSnomedId, final Integer namespaceId, final String partitionId, final String releaseId, final String executionId, final String moduleId) throws RemoteException, CreateConceptIdsFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(final HashMap<UUID, String> componentUUIDandParentSnomedId, final Integer namespaceId, final String partitionId, final String releaseId, final String executionId, final String moduleId) throws RemoteException, CreateConceptIDListFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	public void reset() {
		sctid = 800000L;
		snomedIdCounter = 1;
		ctv3IdChar = 64;
	}

	@Override
	public Map<UUID, String> createCTV3IDList(final List<UUID> componentUuidList)
			throws RemoteException, CreateCTV3IDListFaultException {
		final Map<UUID, String> result =  new HashMap<>();
		for (final UUID uuid : componentUuidList) {
			do {
				ctv3IdChar++;
			} while(!Character.isAlphabetic(ctv3IdChar) && ctv3IdChar < 123);
			if (ctv3IdChar > 122) {
				ctv3IdChar = 64;
			}
			result.put(uuid, CTV3ID_PREFIX + Character.valueOf((char)(ctv3IdChar)));
		}
		return result;
	}

	@Override
	public Map<UUID, String> getCTV3IDList(final List<UUID> componentUuidList)
			throws RemoteException, GetCTV3IDListFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public Map<UUID, String> getSNOMEDIDList(final List<UUID> componentUuidList)
			throws RemoteException, GetSnomedIDListFaultException {
		throw NOT_IMPLEMENTED_EXCEPTION;
	}

	@Override
	public Map<Long, String> createSNOMEDIDList( final Long[][] sctIdAndParentSctIdArray) throws RemoteException, CreateSnomedIDListFaultException {
		final Map<Long, String> result =  new HashMap<>();
		for (int i = 0; i < sctIdAndParentSctIdArray.length; i++) {
			final Long sctId = sctIdAndParentSctIdArray[i][0];
			String hexString = Integer.toHexString(snomedIdCounter++);
			final int numberOfZeorsToPadd = 4 - hexString.length();
			if (hexString.length() < 4) {
				for (int j = 0; j < numberOfZeorsToPadd; j++) {
					hexString = "0" + hexString;
				}
			}
			result.put(sctId, SNOMED_ID_PREFIX + hexString);
		}
		return result;
	}

}
