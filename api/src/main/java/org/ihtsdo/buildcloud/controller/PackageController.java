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

@Controller
@RequestMapping("/builds/{buildCompositeKey}/packages")
public class PackageController {

	@Autowired
	private PackageService packageService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PACKAGE_LINKS = {"input-files"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getPackages(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Package> packages = packageService.findAll(buildCompositeKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(packages, request, PACKAGE_LINKS);
	}

	@RequestMapping("/{packageBusinessKey}")
	@ResponseBody
	public Map getPackage(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Package aPackage = packageService.find(buildCompositeKey, packageBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(aPackage, request, PACKAGE_LINKS);
	}

}
