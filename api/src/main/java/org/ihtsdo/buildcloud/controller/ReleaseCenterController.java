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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.mangofactory.swagger.annotations.ApiIgnore;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/centers")
@Api(value = "Release Center", position = 3)
public class ReleaseCenterController {

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Autowired
	private PublishService publishService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_CENTER_LINKS = {"products", "published"};

	@RequestMapping( method = RequestMethod.GET )
	@ApiOperation( value = "Returns a list all release center for a logged in user",
		notes = "Returns a list of all release centers visible to the currently logged in user." )
	@ResponseBody
	public List<Map<String, Object>> getReleaseCenters(HttpServletRequest request) {
		List<ReleaseCenter> centers = releaseCenterService.findAll();
		return hypermediaGenerator.getEntityCollectionHypermedia(centers, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ApiOperation( value = "Returns a list all release center for a logged in user",
		notes = " Creates a new Release Center and returns the newly created release center." )
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
	@ApiOperation( value = "Updates a release center details",
		notes = "Allows the name, shortName and the visibility of a release center (soft delete) to be changed.   "
				+ "Note that the short name is used in the formation of the ‘business key'" )
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

	@RequestMapping( value = "/{releaseCenterBusinessKey}", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a single release center",
		notes = "Returns a single release center for given releaseCenterBusinessKey" )
	@ResponseBody
	public Map<String, Object> getReleaseCenter(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping("/{releaseCenterBusinessKey}/published")
	@ResponseBody
	@ApiIgnore
	public Map<String, Object> getReleaseCenterPublishedPackages(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		List<String> publishedPackages = publishService.getPublishedPackages(center);
		Map<String, Object> representation = new HashMap<>();
		representation.put("publishedPackages", publishedPackages);
		return hypermediaGenerator.getEntityHypermedia(representation, true, request);
	}

	@RequestMapping(value = "/{releaseCenterBusinessKey}/published", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	@ApiIgnore
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
