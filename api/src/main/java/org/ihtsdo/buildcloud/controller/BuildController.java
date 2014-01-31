package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class BuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] BUILD_LINKS = {"packages"};

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds")
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
											   @PathVariable String productBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<Build> builds = buildService.findAll(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds/{buildBusinessKey}")
	@ResponseBody
	public Map getBuild(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
						@PathVariable String productBusinessKey, @PathVariable String buildBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Build build = buildService.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(build, request, BUILD_LINKS);
	}

}
