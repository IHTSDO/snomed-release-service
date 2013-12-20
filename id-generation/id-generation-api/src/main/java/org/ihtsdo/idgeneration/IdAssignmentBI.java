package org.ihtsdo.idgeneration;

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
	 * @throws Exception
	 */
	public Long getSCTID(UUID componentUuid) throws Exception;

	/**
	 * Returns the component SnomedID
	 * @param componentUuid
	 * @return SnomedID
	 * @throws Exception
	 */
	public String getSNOMEDID(UUID componentUuid) throws Exception;

	/**
	 * Returns the component CTV3ID
	 * @param componentUuid
	 * @return CTV3ID
	 * @throws Exception
	 */
	public String getCTV3ID(UUID componentUuid) throws Exception;

	/**
	 * Returns a map of componentUUID - SCTID for every componentUUID of the parameter
	 * @param componentUuidList
	 * @return componentUUID - SCTID pairs 
	 * @throws Exception
	 */
	public HashMap<UUID, Long> getSCTIDList(List<UUID> componentUuidList) throws Exception;

	/**
	 * Creates and returns SCTID
	 * @param componentUuid
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return SCTID
	 * @throws Exception
	 */
	public Long createSCTID(UUID componentUuid, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws Exception;

	/**
	 * Creates and returns SNOMEDID
	 * @param componentUuid
	 * @param parentSnomedId
	 * @return SNOMEDID
	 * @throws Exception
	 */
	public String createSNOMEDID(UUID componentUuid, String parentSnomedId) throws Exception;

	/**
	 * Creates and returns CTV3ID
	 * @param componentUuid
	 * @return CTV3ID
	 * @throws Exception
	 */
	public String createCTV3ID(UUID componentUuid) throws Exception;
	
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
	 * @throws Exception
	 */
	public HashMap<IDENTIFIER, String> createConceptIds(UUID componentUuid, String parentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId, String executionId, 
			String moduleId) throws Exception;
	
	/**
	 * Returns componentUUID - SCTID map
	 * @param componentUuidList
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return componentUUID - SCTID map
	 * @throws Exception
	 */
	public HashMap<UUID, Long> createSCTIDList(List<UUID> componentUuidList, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws Exception;

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
	 * @throws Exception
	 */
	public HashMap<UUID, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<UUID, String> componentUUIDandParentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId,
			String executionId, String moduleId) throws Exception;

}
