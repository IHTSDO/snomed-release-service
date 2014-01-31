package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ReleaseCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
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

	@RequestMapping("/{releaseCentreBusinessKey}")
	@ResponseBody
	public Map getReleaseCentre(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCentre centre = releaseCentreService.find(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(centre, request, RELEASE_CENTRE_LINKS);
	}

}
