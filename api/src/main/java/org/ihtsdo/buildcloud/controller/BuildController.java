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

@Controller
@RequestMapping("/builds")
public class BuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	static final String[] BUILD_LINKS = {"packages", "config"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Build> builds = buildService.findAll(authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/{buildCompositeKey}")
	@ResponseBody
	public Map getBuild(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Build build = buildService.find(buildCompositeKey, authenticatedId);

		return hypermediaGenerator.getEntityHypermedia(build, request, BUILD_LINKS);
	}
	
	@RequestMapping("/{buildCompositeKey}/config")
	@ResponseBody
	public Map getBuildConfig(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Map config = buildService.getConfig(buildCompositeKey, authenticatedId);
		return config;
	}	

}
