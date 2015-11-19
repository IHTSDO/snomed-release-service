package org.ihtsdo.buildcloud.service.identifier.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.otf.rest.client.RestClientException;


public interface IdServiceRestClient {

	Long getOrCreateSctId(UUID componentUuid, Integer namespaceId, String partitionId, String comment) throws RestClientException;

	Map<UUID,Long> getOrCreateSctIds(List<UUID> uuids,Integer namespaceId,String partitionId, String comment) throws RestClientException;

	Map<UUID, String> getOrCreateSchemeIds(List<UUID> uuids,SchemeIdType schemeType, String comment) throws RestClientException;

	String logIn() throws RestClientException;
	
	void logOut() throws RestClientException;

}
