package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HypermediaGenerator {

	private final ObjectMapper mapper;

	public HypermediaGenerator() {
		mapper = new ObjectMapper();
	}

	public List<Map<String, Object>> getEntityCollectionHypermedia(Collection<? extends Object> entities, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		List<Map<String, Object>> entitiesHypermedia = new ArrayList<>();
		for (Object entity : entities) {
			entitiesHypermedia.add(getEntityHypermedia(entity, false, url, entityLinks));
		}
		return entitiesHypermedia;
	}

	public Map<String, Object> getEntityHypermedia(Object entity, HttpServletRequest request, String... entityLinks) {
		String url = getUrl(request);
		return getEntityHypermedia(entity, true, url, entityLinks);
	}

	private Map<String, Object> getEntityHypermedia(Object entity, boolean currentResource, String url, String... entityLinks) {
		Map<String,Object> entityMap = mapper.convertValue(entity, Map.class);
		if (!currentResource) {
			url = url + "/" + entityMap.get("id");
		}
		if (entityMap != null) {
			entityMap.put("url", url);
			for (String link : entityLinks) {
				entityMap.put(link + "_url", url + "/" + link);
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

}
