package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers/{releaseCenterBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds")
public class ProductBuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String NAME = "name";

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCenterBusinessKey,
			@PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey,
			HttpServletRequest request) throws ResourceNotFoundException {

		List<Build> builds = buildService.findForProduct(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS, "/builds");
	}

	@RequestMapping("/{buildBusinessKey}")
	@ResponseBody
	public Map<String, Object> getBuild(@PathVariable String releaseCenterBusinessKey,
			@PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey,
			@PathVariable String buildBusinessKey,
			HttpServletRequest request) throws ResourceNotFoundException {

		Build build = buildService.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey);
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BuildController.BUILD_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable String releaseCenterBusinessKey,
			@PathVariable String extensionBusinessKey,
			@PathVariable String productBusinessKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}
		String name = json.get(NAME);

		Build build = buildService.create(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, name);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BuildController.BUILD_LINKS), HttpStatus.CREATED);
	}

}
