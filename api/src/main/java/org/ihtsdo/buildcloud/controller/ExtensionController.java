package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.controller.helper.LinkPath;
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
@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions")
public class ExtensionController {

	@Autowired
	private ExtensionService extensionService;
	
	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final LinkPath [] EXTENSION_LINKS_PATHS = { new LinkPath ("products") ,
															   new LinkPath ("starredBuilds", null, "builds?starred=true") };

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Extension> extensions = extensionService.findAll(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS_PATHS);
	}

	@RequestMapping("/{extensionBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.find(releaseCentreBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(extension, request, EXTENSION_LINKS_PATHS);
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createExtension(@PathVariable String releaseCentreBusinessKey,
											   @RequestBody(required = false) Map<String, String> json,
											   HttpServletRequest request) throws IOException {

		String name = json.get("name");

		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.create(releaseCentreBusinessKey, name, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermediaJustCreated(extension, request, EXTENSION_LINKS_PATHS );
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}
	
	@RequestMapping("/{extensionBusinessKey}/builds")
	@ResponseBody
	public List<Map<String, Object>> getBuilds( @PathVariable String releaseCentreBusinessKey, 
												@PathVariable String extensionBusinessKey,
												@RequestParam(value="includeRemoved", required=false) String includeRemovedStr,
												@RequestParam(value="starred", required=false) String starredStr,
												HttpServletRequest request) {
		
		EnumSet <FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (Boolean.parseBoolean(includeRemovedStr)) filterOptions.add(FilterOption.INCLUDE_REMOVED);
		if (Boolean.parseBoolean(starredStr)) filterOptions.add(FilterOption.STARRED_ONLY);
		
		String authenticatedId = SecurityHelper.getSubject();
		List<Build> builds = buildService.findForExtension(releaseCentreBusinessKey, extensionBusinessKey, filterOptions, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS, "/builds");
	}
	
	/**
	 * Having a problem getting ember to pass filter like parameters for nested resources.
	 * Until we can get this sorted out, will be doing the filtering manually.
	 * @param releaseCentreBusinessKey
	 * @param extensionBusinessKey
	 * @param request
	 * @return
	 */
	@RequestMapping("/{extensionBusinessKey}/starredBuilds")
	@ResponseBody
	public List<Map<String, Object>> getBuilds( @PathVariable String releaseCentreBusinessKey, 
												@PathVariable String extensionBusinessKey,
												HttpServletRequest request) {
		
		return getBuilds (releaseCentreBusinessKey, extensionBusinessKey, "false", "true", request);
	}

}
