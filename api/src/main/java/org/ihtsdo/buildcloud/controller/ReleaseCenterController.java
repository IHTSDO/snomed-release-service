package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ReleaseCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers")
public class ReleaseCenterController {

	@Autowired
	private ReleaseCenterService releaseCenterService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_CENTER_LINKS = {"extensions"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getReleaseCenters(HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<ReleaseCenter> centers = releaseCenterService.findAll(authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(centers, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createReleaseCenter(@RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws IOException {
		String name = json.get("name");
		String shortName = json.get("shortName");

		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCenter center = releaseCenterService.create(name, shortName, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermediaJustCreated(center, request, RELEASE_CENTER_LINKS);
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{releaseCenterBusinessKey}", method = RequestMethod.PUT, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map updateReleaseCenter(@PathVariable String releaseCenterBusinessKey,
												   @RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws IOException {

		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCenter center = releaseCenterService.find(releaseCenterBusinessKey, authenticatedId);
		center.setName(json.get("name"));
		center.setShortName(json.get("shortName"));
		center.setRemoved("true".equalsIgnoreCase(json.get("removed")));
		releaseCenterService.update(center);
		return hypermediaGenerator.getEntityHypermedia(center, request, RELEASE_CENTER_LINKS);
	}

	@RequestMapping("/{releaseCenterBusinessKey}")
	@ResponseBody
	public Map getReleaseCenter(@PathVariable String releaseCenterBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCenter center = releaseCenterService.find(releaseCenterBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(center, request, RELEASE_CENTER_LINKS);
	}

}
