package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.PackageService;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

	private static final String[] PACKAGE_LINKS = {"manifest", "inputfiles"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getPackages(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		User authenticatedUser = SecurityHelper.getSubject();
		List<Package> packages = packageService.findAll(buildCompositeKey, authenticatedUser);
		return hypermediaGenerator.getEntityCollectionHypermedia(packages, request, PACKAGE_LINKS);
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createPackage(@PathVariable String buildCompositeKey, @RequestBody(required = false) Map<String, String> json, HttpServletRequest request) throws BadRequestException {
		User authenticatedUser = SecurityHelper.getSubject();
		String name = json.get("name");
		if (name != null) {
			Package aPackage = packageService.create(buildCompositeKey, name, authenticatedUser);
			return hypermediaGenerator.getEntityHypermedia(aPackage, false, request, PACKAGE_LINKS);
		} else {
			throw new BadRequestException("Package name is required.");
		}
	}

	@RequestMapping("/{packageBusinessKey}")
	@ResponseBody
	public Map getPackage(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey, HttpServletRequest request) {

		User authenticatedUser = SecurityHelper.getSubject();
		Package aPackage = packageService.find(buildCompositeKey, packageBusinessKey, authenticatedUser);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(aPackage, currentResource, request, PACKAGE_LINKS);
	}

	@RequestMapping(value = "/{packageBusinessKey}", method = RequestMethod.PATCH, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map updatePackage(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							 @RequestBody(required = false) Map<String, String> json, HttpServletRequest request) {
		User authenticatedUser = SecurityHelper.getSubject();
		Package aPackage = packageService.update(buildCompositeKey, packageBusinessKey, json, authenticatedUser);
		return hypermediaGenerator.getEntityHypermedia(aPackage, true, request, PACKAGE_LINKS);
	}

}
