package org.ihtsdo.idgeneration;

import org.ihtsdo.idgen.ws.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public interface IdAssignmentBI {

	public enum IDENTIFIER {
		TDEUID(1), SCTID(2), SNOMEDID(3), CTV3ID(4), WBUID(5);
		
		private int idNumber;
		
		IDENTIFIER(int idNumber){
			this.setIdNumber(idNumber);
		}

		public void setIdNumber(int idNumber) {
			this.idNumber = idNumber;
		}

		public int getIdNumber() {
			return idNumber;
		}
		
	}

	/**
	 * Returns the SCTID related to the component UUID
	 * @param componentUuid the component UUID
	 * @return the SCTID
	 * @throws GetSCTIDFault, RemoteException
	 */
	public Long getSCTID(UUID componentUuid) throws GetSCTIDFault, RemoteException;

	/**
	 * Returns the component SnomedID
	 * @param componentUuid
	 * @return SnomedID
	 * @throws RemoteException, GetSNOMEDIDFault
	 */
	public String getSNOMEDID(UUID componentUuid) throws RemoteException, GetSNOMEDIDFault;

	/**
	 * Returns the component CTV3ID
	 * @param componentUuid
	 * @return CTV3ID
	 * @throws GetCTV3IDFault, RemoteException
	 */
	public String getCTV3ID(UUID componentUuid) throws GetCTV3IDFault, RemoteException;

	/**
	 * Returns a map of componentUUID - SCTID for every componentUUID of the parameter
	 * @param componentUuidList
	 * @return componentUUID - SCTID pairs 
	 * @throws RemoteException, GetSCTIDListFault
	 */
	public HashMap<UUID, Long> getSCTIDList(List<UUID> componentUuidList) throws RemoteException, GetSCTIDListFault;

	/**
	 * Creates and returns SCTID
	 * @param componentUuid
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return SCTID
	 * @throws CreateSCTIDFaultException, RemoteException
	 */
	public Long createSCTID(UUID componentUuid, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws CreateSCTIDFaultException, RemoteException;

	/**
	 * Creates and returns SNOMEDID
	 * @param componentUuid
	 * @param parentSnomedId
	 * @return SNOMEDID
	 * @throws RemoteException, CreateSNOMEDIDFaultException
	 */
	public String createSNOMEDID(UUID componentUuid, String parentSnomedId) throws RemoteException, CreateSNOMEDIDFaultException;

	/**
	 * Creates and returns CTV3ID
	 * @param componentUuid
	 * @return CTV3ID
	 * @throws RemoteException, CreateCTV3IDFaultException
	 */
	public String createCTV3ID(UUID componentUuid) throws RemoteException, CreateCTV3IDFaultException;
	
	/**
	 * Returns identifier-conceptid map  
	 * @param componentUuid
	 * @param parentSnomedId
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return Concept Ids
	 * @throws RemoteException, CreateConceptIdsFaultException
	 */
	public HashMap<IDENTIFIER, String> createConceptIds(UUID componentUuid, String parentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId, String executionId, 
			String moduleId) throws RemoteException, CreateConceptIdsFaultException;
	
	/**
	 * Returns componentUUID - SCTID map
	 * @param componentUuidList
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return componentUUID - SCTID map
	 * @throws RemoteException, CreateSCTIDListFaultException
	 */
	public HashMap<UUID, Long> createSCTIDList(List<UUID> componentUuidList, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws RemoteException, CreateSCTIDListFaultException;

	/**
	 * For every componentUUID and ParentSnomedID pair returns <br> 
	 * a map of the componentUUID - (identifier-conceptid) map.
	 * @param componentUUIDandParentSnomedId
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return <ComponentUUID, HashMap<IDENTIFIER, concpetid>>
	 * @throws RemoteException, CreateConceptIDListFaultException
	 */
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<UUID, String> componentUUIDandParentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId,
			String executionId, String moduleId) throws RemoteException, CreateConceptIDListFaultException;

}
