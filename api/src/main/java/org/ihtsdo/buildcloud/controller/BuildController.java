package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public List<Map<String, Object>> getBuilds(@RequestParam(value="includeRemoved", required=false) String includeRemovedStr,
			@RequestParam(value="starred", required=false) String starredStr,
			HttpServletRequest request) {
		
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (Boolean.parseBoolean(includeRemovedStr)) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}
		if (Boolean.parseBoolean(starredStr)) {
			filterOptions.add(FilterOption.STARRED_ONLY);
		}
		
		List<Build> builds = buildService.findAll(filterOptions);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/{buildCompositeKey}")
	@ResponseBody
	public Map<String, Object> getBuild(@PathVariable String buildCompositeKey, HttpServletRequest request) throws BusinessServiceException {
		Build build = buildService.find(buildCompositeKey);
		
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " +  buildCompositeKey);
		}
		
		return hypermediaGenerator.getEntityHypermedia(build, true, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildCompositeKey}", method = RequestMethod.PATCH, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map<String, Object> updateBuild(@PathVariable String buildCompositeKey, @RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {
		Build build = buildService.update(buildCompositeKey, json);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " +  buildCompositeKey);
		}
		return hypermediaGenerator.getEntityHypermedia(build, true, request, BUILD_LINKS);
	}
	
}
