package com.termmed.genid;

import java.util.HashMap;
import java.util.List;


public interface SctIdGen {
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

	public Long getSCTID(String componentUuid) throws Exception;

	public String getSNOMEDID(String componentUuid) throws Exception;

	public String getCTV3ID(String componentUuid) throws Exception;

	public HashMap<String, Long> getSCTIDList(List<String> componentUuidList) throws Exception;

	/**
	 * Creates SCTID
	 * @param componentUuid
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return
	 * @throws Exception
	 */
	public Long createSCTID(String componentUuid, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws Exception;

	public String createSNOMEDID(String componentUuid, String parentSnomedId) throws Exception;

	public String createCTV3ID(String componentUuid) throws Exception;

	public HashMap<IDENTIFIER, String> createConceptIds(String componentUuid, String parentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId, String executionId, 
			String moduleId) throws Exception;

	public HashMap<String, Long> createSCTIDList(List<String> componentUuidList, Integer namespaceId, String partitionId, 
			String releaseId, String executionId, String moduleId) throws Exception;

	/**
	 * 
	 * @param componentUUIDandParentSnomedId
	 * @param namespaceId
	 * @param partitionId
	 * @param releaseId
	 * @param executionId
	 * @param moduleId
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, HashMap<IDENTIFIER, String>> createConceptIDList(HashMap<String, String> componentUUIDandParentSnomedId, 
			Integer namespaceId, String partitionId, String releaseId,
			String executionId, String moduleId) throws Exception;
}
