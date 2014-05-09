package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/builds")
public class BuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] BUILD_LINKS = {"packages", "executions"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds( @RequestParam(value="includeRemoved", required=false) String includeRemovedStr,
												@RequestParam(value="starred", required=false) String starredStr,
												HttpServletRequest request) {
		
		EnumSet <FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (Boolean.parseBoolean(includeRemovedStr)) filterOptions.add(FilterOption.INCLUDE_REMOVED);
		if (Boolean.parseBoolean(starredStr)) filterOptions.add(FilterOption.STARRED_ONLY);
		
		User authenticatedUser = SecurityHelper.getSubject();
		List<Build> builds = buildService.findAll(filterOptions, authenticatedUser);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/{buildCompositeKey}")
	@ResponseBody
	public Map getBuild(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		User authenticatedUser = SecurityHelper.getSubject();
		Build build = buildService.find(buildCompositeKey, authenticatedUser);
		boolean currentResource = false;
		return hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS);
	}
	
}
