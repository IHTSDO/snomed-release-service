package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions")
public class ExtensionController {

	@Autowired
	private ExtensionService extensionService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] EXTENSION_LINKS = {"products"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Extension> extensions = extensionService.findAll(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS);
	}

	@RequestMapping("/{extensionBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.find(releaseCentreBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(extension, request, EXTENSION_LINKS);
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createExtension(@PathVariable String releaseCentreBusinessKey,
											 @RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws IOException {

		String name = json.get("name");

		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.create(releaseCentreBusinessKey, name, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(extension, request, EXTENSION_LINKS);
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}		

}
