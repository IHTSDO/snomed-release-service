package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
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

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCenterBusinessKey,
											   @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey,
											   HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Build> builds = buildService.findForProduct(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS, "/builds");
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createBuild(@PathVariable String releaseCenterBusinessKey,
											 @PathVariable String extensionBusinessKey,
											 @PathVariable String productBusinessKey,											 
											 @RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws Exception {
		//TODO PETER - Return 404 rather than throw exception if extension not found.
		String name = json.get("name");

		String authenticatedId = SecurityHelper.getSubject();
		Build build = buildService.create(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, name, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermediaJustCreated(build, request, BuildController.BUILD_LINKS);
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}		

}
