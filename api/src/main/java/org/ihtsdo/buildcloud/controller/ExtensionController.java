package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ExtensionController {

	@Autowired
	private ExtensionService extensionService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] EXTENSION_LINKS = {"products"};

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions")
	@ResponseBody
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<Extension> extensions = extensionService.findAll(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Extension extension = extensionService.find(releaseCentreBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(extension, request, EXTENSION_LINKS);
	}

}
