package org.ihtsdo.buildcloud.service.identifier.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.otf.rest.client.RestClientException;


public interface IdServiceRestClient {
	
	enum  BULK_JOB_STATUS {
		PENDING (0),
		RUNNING (1),
		COMPLETED_WITH_SUCCESS (2),
		COMPLETED_WITH_ERROR (3);
		
		private int code;
		
		private BULK_JOB_STATUS(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}
	
	enum ID_STATUS {
		AVAILABLE("Available"),
		ASSIGNED("Assigned"),
		RESERVED("Reserved"),
		DEPRECATED("Deprecated"),
		PUBLISHED("Published");
		
		private String name;
		
		ID_STATUS (String name) {
			this.name = name;
		}
		public String getName() {
			return this.name;
		}
	}

	Long getOrCreateSctId(UUID componentUuid, Integer namespaceId, String partitionId, String comment) throws RestClientException;

	Map<UUID,Long> getOrCreateSctIds(List<UUID> uuids,Integer namespaceId,String partitionId, String comment) throws RestClientException;

	Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids,SchemeIdType schemeType, String comment) throws RestClientException;

	String logIn() throws RestClientException;
	
	void logOut() throws RestClientException;

	boolean publishSctIds(List<Long> sctids, Integer namespaceId, String comment) throws RestClientException;

	boolean publishSchemeIds(List<String> schemeIds, SchemeIdType schemeType, String comment) throws RestClientException;

	Map<Long, String> getSctidStatusMap(Collection<Long> sctIds) throws RestClientException;

	Map<String, String> getSchemeIdStatusMap(SchemeIdType schemeType, Collection<String> legacyIds) throws RestClientException;

}
