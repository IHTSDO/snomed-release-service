package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class HypermediaGenerator {

	@Autowired
	private ObjectMapper objectMapper;

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<?> entities, HttpServletRequest request, String[] entityLinks) {
		return getEntityCollectionHypermedia(entities, request, entityLinks, null);
	}

	public List<Map<String,Object>> getEntityCollectionHypermedia(Collection<?> entities, HttpServletRequest request) {
		return getEntityCollectionHypermedia(entities, request, null, null);
	}

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<?> entities, HttpServletRequest request, String[] entityLinks, String instanceRoot) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		if (instanceRoot != null) {
			url = apiRootUrl + instanceRoot;
		}
		List<Map<String, Object>> entitiesHypermedia = new ArrayList<>();
		for (Object entity : entities) {
			entitiesHypermedia.add(getEntityHypermedia(entity, false, url, apiRootUrl, entityLinks));
		}
		return entitiesHypermedia;
	}

	/**
	 *
	 * @param entity
	 * @param currentResource - if true indicates that the URL already contains the entity id.  If false, the entity will be added.
	 * @param request
	 * @param entityLinks
	 * @return
	 */
	public Map<String, Object> getEntityHypermedia(Object entity, boolean currentResource, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, currentResource, url, apiRootUrl, entityLinks);
	}

	public Map<String, Object> getEntityHypermediaOfAction(Object entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		// Remove action name
		url = url.substring(0, url.lastIndexOf("/"));
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, true, url, apiRootUrl, entityLinks);
	}

	private Map<String, Object> getEntityHypermedia(Object entity, boolean currentResource, String url, String apiRootUrl, String... entityLinks) {


		Map<String,Object> entityMap = objectMapper.convertValue(entity, Map.class);
		if (!currentResource) {
			url = url + "/" + entityMap.get("id");
		}
		if (entityMap != null) {
			entityMap.put("url", url);
			if (entityLinks != null) {
				for (String link : entityLinks) {
					String linkName;
					if (link.contains("|")) {
						String[] linkParts = link.split("\\|");
						linkName = linkParts[0];
						link = linkParts[1];
					} else {
						linkName = link.replace("/", "");
					}
					String linkUrl = (link.startsWith("/") ? apiRootUrl : (url + "/")) + link;
					entityMap.put(linkName + "_url", linkUrl);
				}
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
}
