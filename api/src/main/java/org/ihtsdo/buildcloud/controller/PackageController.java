package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.PackageService;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
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
@RequestMapping("/builds/{buildCompositeKey}/packages")
public class PackageController {

	@Autowired
	private PackageService packageService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PACKAGE_LINKS = {"manifest", "inputfiles"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getPackages(@PathVariable String buildCompositeKey, HttpServletRequest request) throws ResourceNotFoundException {
		List<Package> packages = packageService.findAll(buildCompositeKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(packages, request, PACKAGE_LINKS);
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createPackage(@PathVariable String buildCompositeKey, @RequestBody(required = false) Map<String, String> json, HttpServletRequest request) throws BadRequestException, EntityAlreadyExistsException, ResourceNotFoundException {
		String name = json.get("name");
		if (name != null) {
			Package aPackage = packageService.create(buildCompositeKey, name);
			return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(aPackage, false, request, PACKAGE_LINKS), HttpStatus.CREATED);
		} else {
			throw new BadRequestException("Package name is required.");
		}
	}

	@RequestMapping("/{packageBusinessKey}")
	@ResponseBody
	public Map<String, Object> getPackage(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		Package aPackage = packageService.find(buildCompositeKey, packageBusinessKey);

		if (aPackage == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, packageBusinessKey);
			throw new ResourceNotFoundException("Unable to find package: " + item);
		}

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(aPackage, currentResource, request, PACKAGE_LINKS);
	}

	@RequestMapping(value = "/{packageBusinessKey}", method = RequestMethod.PATCH, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map<String, Object> updatePackage(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
			@RequestBody(required = false) Map<String, String> json, HttpServletRequest request)
			throws ResourceNotFoundException, BadConfigurationException {

		Package aPackage = packageService.update(buildCompositeKey, packageBusinessKey, json);
		if (aPackage == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, packageBusinessKey);
			throw new ResourceNotFoundException("Unable to find package: " + item);
		}
		return hypermediaGenerator.getEntityHypermedia(aPackage, true, request, PACKAGE_LINKS);
	}

}
