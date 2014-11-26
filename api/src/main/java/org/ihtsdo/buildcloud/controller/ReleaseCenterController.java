package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.ReleaseCenterService;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/centers")
public class ReleaseCenterController {

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Autowired
	private PublishService publishService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_CENTER_LINKS = {"products", "published"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getReleaseCenters(HttpServletRequest request) {
		List<ReleaseCenter> centers = releaseCenterService.findAll();
		return hypermediaGenerator.getEntityCollectionHypermedia(centers, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createReleaseCenter(@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws IOException, EntityAlreadyExistsException {

		String name = json.get("name");
		String shortName = json.get("shortName");
		ReleaseCenter center = releaseCenterService.create(name, shortName);

		boolean currentResource = false;
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
		return new ResponseEntity<>(entityHypermedia, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{releaseCenterBusinessKey}", method = RequestMethod.PUT, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map<String, Object> updateReleaseCenter(@PathVariable String releaseCenterBusinessKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = releaseCenterService.find(releaseCenterBusinessKey);
		center.setName(json.get("name"));
		center.setShortName(json.get("shortName"));
		center.setRemoved("true".equalsIgnoreCase(json.get("removed")));
		releaseCenterService.update(center);
		boolean currentResource = false;
		return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping("/{releaseCenterBusinessKey}")
	@ResponseBody
	public Map<String, Object> getReleaseCenter(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping("/{releaseCenterBusinessKey}/published")
	@ResponseBody
	public Map<String, Object> getReleaseCenterPublishedPackages(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		List<String> publishedPackages = publishService.getPublishedPackages(center);
		Map<String, Object> representation = new HashMap<>();
		representation.put("publishedPackages", publishedPackages);
		return hypermediaGenerator.getEntityHypermedia(representation, true, request);
	}

	@RequestMapping(value = "/{releaseCenterBusinessKey}/published", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public ResponseEntity<Object> publishReleaseCenterPackage(@PathVariable String releaseCenterBusinessKey,
			@RequestParam(value = "file") final MultipartFile file) throws BusinessServiceException, IOException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		try (InputStream inputStream = file.getInputStream()) {
			publishService.publishAdHocFile(center, inputStream, file.getOriginalFilename(), file.getSize());
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	private ReleaseCenter getReleaseCenterRequired(String releaseCenterBusinessKey) throws ResourceNotFoundException {
		ReleaseCenter center = releaseCenterService.find(releaseCenterBusinessKey);
		if (center == null) {
			throw new ResourceNotFoundException("Unable to find release center: " + releaseCenterBusinessKey);
		}
		return center;
	}

}
