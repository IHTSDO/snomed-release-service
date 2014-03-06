package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class HypermediaGenerator {

	private final ObjectMapper mapper;

	public HypermediaGenerator(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends Object> entities, HttpServletRequest request, String[] entityLinks) {
		return  getEntityCollectionHypermedia(entities, request, entityLinks, null);
	}

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends Object> entities, HttpServletRequest request, String[] entityLinks, String instanceRoot) {
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

	public Map<String, Object> getEntityHypermedia(Object entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, true, url, apiRootUrl, entityLinks);
	}

	public Map<String, Object> getEntityHypermediaJustCreated(Object entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		String apiRootUrl = getApiRootUrl(url, request);
		return getEntityHypermedia(entity, false, url, apiRootUrl, entityLinks);
	}

	private Map<String, Object> getEntityHypermedia(Object entity, boolean currentResource, String url, String apiRootUrl, String... entityLinks) {
		Map<String,Object> entityMap = mapper.convertValue(entity, Map.class);
		if (!currentResource) {
			url = url + "/" + entityMap.get("id");
		}
		if (entityMap != null) {
			entityMap.put("url", url);
			for (String link : entityLinks) {
				String linkUrl = (link.startsWith("/") ? apiRootUrl : (url + "/") ) + link;
				link = link.replace("/", "");
				entityMap.put(link + "_url", linkUrl);
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
