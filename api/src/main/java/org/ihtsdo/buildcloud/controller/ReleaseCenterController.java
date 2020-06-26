package org.ihtsdo.buildcloud.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.ReleaseCenterService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

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

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation( value = "Creates a new Release Center for a logged in user",
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

	@RequestMapping(value = "/{releaseCenterBusinessKey}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
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
		notes = "Returns a single release center for a given releaseCenterBusinessKey" )
	@ResponseBody
	public Map<String, Object> getReleaseCenter(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(center, currentResource, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping(value = "/{releaseCenterBusinessKey}/published", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a list published releases names",
		notes = "Returns a list published releases names for a given release center" )
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
	@ApiIgnore
	public ResponseEntity<Object> publishReleaseCenterPackage(@PathVariable String releaseCenterBusinessKey,
			@RequestParam(value = "file") final MultipartFile file, @RequestParam(value = "isComponentIdPublishingRequired", defaultValue ="true") boolean publishComponentIds ) throws BusinessServiceException, IOException {

		ReleaseCenter center = getReleaseCenterRequired(releaseCenterBusinessKey);

		try (InputStream inputStream = file.getInputStream()) {
			publishService.publishAdHocFile(center, inputStream, file.getOriginalFilename(), file.getSize(), publishComponentIds);
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
