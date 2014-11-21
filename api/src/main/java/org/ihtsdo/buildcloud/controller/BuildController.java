package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/builds")
public class BuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] BUILD_LINKS = {"executions"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCenterKey, @RequestParam(required=false) boolean includeRemoved,
			HttpServletRequest request) {
		
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (includeRemoved) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}

		List<Build> builds = buildService.findAll(releaseCenterKey, filterOptions);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/{buildKey}")
	@ResponseBody
	public Map<String, Object> getBuild(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			HttpServletRequest request) throws BusinessServiceException {
		Build build = buildService.find(releaseCenterKey, buildKey);
		
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " +  buildKey);
		}
		
		return hypermediaGenerator.getEntityHypermedia(build, true, request, BUILD_LINKS);
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable String releaseCenterKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}

		String name = json.get(BuildService.NAME);
		Build build = buildService.create(releaseCenterKey, name);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BuildController.BUILD_LINKS), HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{buildKey}", method = RequestMethod.PATCH, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map<String, Object> updateBuild(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		Build build = buildService.update(releaseCenterKey, buildKey, json);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " +  buildKey);
		}
		return hypermediaGenerator.getEntityHypermedia(build, true, request, BUILD_LINKS);
	}
	
}
