package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.ExtensionService;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers/{releaseCenterBusinessKey}/extensions")
public class ExtensionController {

	@Autowired
	private ExtensionService extensionService;
	
	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] EXTENSION_LINKS = {"products"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Extension> extensions = extensionService.findAll(releaseCenterBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS);
	}

	@RequestMapping("/{extensionBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.find(releaseCenterBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(extension, request, EXTENSION_LINKS);
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createExtension(@PathVariable String releaseCenterBusinessKey,
											   @RequestBody(required = false) Map<String, String> json,
											   HttpServletRequest request) throws IOException {

		String name = json.get("name");

		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.create(releaseCenterBusinessKey, name, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermediaJustCreated(extension, request, EXTENSION_LINKS);
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}
	
	@RequestMapping("/{extensionBusinessKey}/builds")
	@ResponseBody
	public List<Map<String, Object>> getBuilds( @PathVariable String releaseCenterBusinessKey,
												@PathVariable String extensionBusinessKey,
												@RequestParam(value="includeRemoved", required=false) String includeRemovedStr,
												@RequestParam(value="starred", required=false) String starredStr,
												HttpServletRequest request) {
		
		EnumSet <FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (Boolean.parseBoolean(includeRemovedStr)) filterOptions.add(FilterOption.INCLUDE_REMOVED);
		if (Boolean.parseBoolean(starredStr)) filterOptions.add(FilterOption.STARRED_ONLY);
		
		String authenticatedId = SecurityHelper.getSubject();
		List<Build> builds = buildService.findForExtension(releaseCenterBusinessKey, extensionBusinessKey, filterOptions, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS);
	}

}
