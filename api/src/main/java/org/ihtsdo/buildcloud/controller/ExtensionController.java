package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.ExtensionService;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
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
import java.util.Set;

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
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {
		List<Extension> extensions = extensionService.findAll(releaseCenterBusinessKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS);
	}

	@RequestMapping("/{extensionBusinessKey}")
	@ResponseBody
	public Map<String, Object> getExtension(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {
		Extension extension = extensionService.find(releaseCenterBusinessKey, extensionBusinessKey);
		
		if (extension == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey,extensionBusinessKey);
			throw new ResourceNotFoundException ("Unable to find extension: " +  item);
		}
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(extension, currentResource, request, EXTENSION_LINKS);
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createExtension(@PathVariable String releaseCenterBusinessKey,
											   @RequestBody(required = false) Map<String, String> json,
											   HttpServletRequest request) throws IOException, ResourceNotFoundException, EntityAlreadyExistsException {

		String name = json.get("name");
		Extension extension = extensionService.create(releaseCenterBusinessKey, name);

		boolean currentResource = false;
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(extension, currentResource, request, EXTENSION_LINKS);
		return new ResponseEntity<>(entityHypermedia, HttpStatus.CREATED);
	}
	
	@RequestMapping("/{extensionBusinessKey}/builds")
	@ResponseBody
	public List<Map<String, Object>> getBuilds( @PathVariable String releaseCenterBusinessKey,
												@PathVariable String extensionBusinessKey,
												@RequestParam(required=false) boolean includeRemoved,
												@RequestParam(required=false) boolean starred,
												HttpServletRequest request) {
		
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (includeRemoved) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}
		if (starred) {
			filterOptions.add(FilterOption.STARRED_ONLY);
		}
		
		List<Build> builds = buildService.findForExtension(releaseCenterBusinessKey, extensionBusinessKey, filterOptions);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS);
	}

}
