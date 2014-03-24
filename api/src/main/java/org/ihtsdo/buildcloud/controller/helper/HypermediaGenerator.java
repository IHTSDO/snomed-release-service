package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

public class HypermediaGenerator {

	@Autowired
	private ObjectMapper objectMapper;
	
	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends DomainEntity> entities, HttpServletRequest request, LinkPath[] entityLinkPaths) {
		return getEntityCollectionHypermedia(entities, request, entityLinkPaths, null); 
	}	

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends DomainEntity> entities, HttpServletRequest request, LinkPath[] entityLinkPaths, String instanceRoot) {
		List<Map<String, Object>> entitiesHypermedia = new ArrayList<>();
		for (DomainEntity entity : entities) {
			entitiesHypermedia.add(getEntityHypermedia(entity, false, false, request, entityLinkPaths, instanceRoot));
		}
		return entitiesHypermedia;
	}

	public Map<String, Object> getEntityHypermedia(DomainEntity entity, HttpServletRequest request, LinkPath[] entityLinkPaths) {
		return getEntityHypermedia(entity, true, false, request, entityLinkPaths, null);
	}

	public Map<String, Object> getEntityHypermediaJustCreated(DomainEntity entity, HttpServletRequest request, LinkPath[] entityLinkPaths) {
		return getEntityHypermedia(entity, false, false, request, entityLinkPaths, null);
	}

	public Map<String, Object> getEntityHypermediaOfAction(DomainEntity entity, HttpServletRequest request, LinkPath[] entityLinkPaths) {
		return getEntityHypermedia(entity, true, true, request, entityLinkPaths, null);
	}

	private Map<String, Object> getEntityHypermedia(DomainEntity entity, boolean currentResource, boolean isAction, HttpServletRequest request, LinkPath[] entityLinkPaths, String instanceRoot) {
		
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		
		if (isAction){
			// Remove action name
			url = url.substring(0, url.lastIndexOf("/"));
		}
		

		if (instanceRoot != null) {
			url = apiRootUrl + instanceRoot;
		}
		
		Map<String,Object> entityMap = objectMapper.convertValue(entity, Map.class);
		//Complexity has increased here because the child collections can have different shortened urls (instanceRoot)
		//from the parent.  So instanceRoot affects this thing, but children pull their instance root from linkPath.instanceRoot
		if (!currentResource) {
			url += "/" + entityMap.get("id");
		}
		
		if (entityMap != null) {
			entityMap.put("url", url);
			if (entity.getParent() != null) {
				entityMap.put("parent_url", getEntityPath(apiRootUrl, entity.getParent()));
			}
			for (LinkPath linkPath : entityLinkPaths) {
				addLinkToMap(entityMap, linkPath, url, apiRootUrl);
			}
		}
		return entityMap;
	}
	
	private void addLinkToMap (Map<String,Object> entityMap, LinkPath linkPath, String url, String apiRootUrl) {

		String link = linkPath.getLink();
		String linkName;
		if (linkPath.hasInstanceRoot()) {
			url = apiRootUrl + linkPath.getInstanceRoot();
		}
		
		if (link.contains("|")) {
			String[] linkParts = link.split("\\|");
			linkName = linkParts[0];
			link = linkParts[1];
		} else {
			linkName = link.replace("/", "");
		}
		String itemSpecifier = linkPath.hasFilter()? linkPath.getFilter() : link;
		String linkUrl = (link.startsWith("/") ? apiRootUrl : (url + "/") ) + itemSpecifier;
		entityMap.put(linkName + "_url", linkUrl);
	}

	private String getUrl(HttpServletRequest request) {
		String requestUrl = request.getRequestURL().toString();
		// Remove any trailing slash
		if (requestUrl.lastIndexOf('/') == requestUrl.length() - 1) {
			requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
		}
		return requestUrl;
	}

	private String getApiRootUrl(String url, HttpServletRequest request) {
		String rootPath = request.getContextPath() + request.getServletPath();
		return url.substring(0, url.indexOf(rootPath) + rootPath.length());
	}
	
	//Works recursively up the domain path to the root object, then constructs the url working back down
	//using the collentionName (plural) followed by the businessKey
	private String getEntityPath(String apiRootUrl, DomainEntity entity) {
		if (entity.getCollectionName() == null)
			return "";
		
		String thisSection = "/" + entity.getCollectionName() + "/" + entity.getBusinessKey();
		if (entity.getParent() == null) {
			return apiRootUrl + thisSection;
		} 
		return getEntityPath(apiRootUrl, entity.getParent()) + thisSection;
	}
}
