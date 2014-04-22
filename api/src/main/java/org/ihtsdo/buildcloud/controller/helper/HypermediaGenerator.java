package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.DomainEntity;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

public class HypermediaGenerator {

	@Autowired
	private ObjectMapper objectMapper;

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends DomainEntity> entities, HttpServletRequest request, String[] entityLinks) {
		return  getEntityCollectionHypermedia(entities, request, entityLinks, null);
	}

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends DomainEntity> entities, HttpServletRequest request, String[] entityLinks, String instanceRoot) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		if (instanceRoot != null) {
			url = apiRootUrl + instanceRoot;
		}
		List<Map<String, Object>> entitiesHypermedia = new ArrayList<>();
		for (DomainEntity entity : entities) {
			entitiesHypermedia.add(getEntityHypermedia(entity, false, url, apiRootUrl, entityLinks));
		}
		return entitiesHypermedia;
	}

	public Map<String, Object> getEntityHypermedia(DomainEntity entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, true, url, apiRootUrl, entityLinks);
	}

	public Map<String, Object> getEntityHypermediaJustCreated(DomainEntity entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		//The id of the object being created is included in the URL, so we don't need it to be added again.  
		//So passing 'current resource' as true to acheive this.
		return getEntityHypermedia(entity, true, url, apiRootUrl, entityLinks);
	}

	public Map<String, Object> getEntityHypermediaOfAction(DomainEntity entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		// Remove action name
		url = url.substring(0, url.lastIndexOf("/"));
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, true, url, apiRootUrl, entityLinks);
	}

	private Map<String, Object> getEntityHypermedia(DomainEntity entity, boolean currentResource, String url, String apiRootUrl, String... entityLinks) {
		Map<String,Object> entityMap = objectMapper.convertValue(entity, Map.class);
		if (!currentResource) {
			url = url + "/" + entityMap.get("id");
		}
		if (entityMap != null) {
			entityMap.put("url", url);

			if (entity.getParent() != null) {
				entityMap.put("parent_url", getEntityPath(apiRootUrl, entity.getParent()));
			}
			
			for (String link : entityLinks) {
				String linkName;
				if (link.contains("|")) {
					String[] linkParts = link.split("\\|");
					linkName = linkParts[0];
					link = linkParts[1];
				} else {
					linkName = link.replace("/", "");
				}
				String linkUrl = (link.startsWith("/") ? apiRootUrl : (url + "/") ) + link;
				entityMap.put(linkName + "_url", linkUrl);
			}
		}
		return entityMap;
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
		
		boolean isShortcut = false; 
		String keyModifier = "";
		if (entity instanceof Build) {
			isShortcut = true;
			keyModifier = ((Build)entity).getId() + "_";
		}
		
		String thisSection = "/" + entity.getCollectionName() + "/" + keyModifier + entity.getBusinessKey();
		if (entity.getParent() == null || isShortcut) {
			return apiRootUrl + thisSection;
		} 
		return getEntityPath(apiRootUrl, entity.getParent()) + thisSection;
	}
}
