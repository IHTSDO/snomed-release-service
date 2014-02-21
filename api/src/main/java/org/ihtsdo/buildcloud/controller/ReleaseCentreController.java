package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ReleaseCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/centres")
public class ReleaseCentreController {

	@Autowired
	private ReleaseCentreService releaseCentreService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_CENTRE_LINKS = {"extensions"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getReleaseCentres(HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<ReleaseCentre> centres = releaseCentreService.findAll(authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(centres, request, RELEASE_CENTRE_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<String> createReleaseCentre(@RequestBody(required = false) Map<String, String> json) throws IOException {
		String name = json.get("name");
		String shortName = json.get("shortName");

		String authenticatedId = SecurityHelper.getSubject();
		releaseCentreService.create(name, shortName, authenticatedId);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		return new ResponseEntity<String>(headers, HttpStatus.CREATED);
	}

	@RequestMapping("/{releaseCentreBusinessKey}")
	@ResponseBody
	public Map getReleaseCentre(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCentre centre = releaseCentreService.find(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(centre, request, RELEASE_CENTRE_LINKS);
	}

}
