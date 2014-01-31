package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.PackageService;
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
public class PackageController {

	@Autowired
	private PackageService packageService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PACKAGE_LINKS = {};

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds/{releaseBusinessKey}/packages")
	@ResponseBody
	public List<Map<String, Object>> getPackages(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
												 @PathVariable String productBusinessKey, @PathVariable String releaseBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<Package> packages = packageService.findAll(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(packages, request, PACKAGE_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds/{releaseBusinessKey}/packages/{packageBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
							@PathVariable String productBusinessKey, @PathVariable String releaseBusinessKey, @PathVariable String packageBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Package aPackage = packageService.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, packageBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(aPackage, request, PACKAGE_LINKS);
	}

}
