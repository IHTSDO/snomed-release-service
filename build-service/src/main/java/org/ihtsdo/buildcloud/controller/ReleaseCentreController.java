package org.ihtsdo.buildcloud.controller;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ReleaseCentreService;
import org.ihtsdo.buildcloud.service.helper.LazyInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ReleaseCentreController {

	@Autowired
	private ReleaseCentreService releaseCentreService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_CENTRE_LINKS = {"extensions"};
	private static final String[] EXTENSION_LINKS = {};

	private static final LazyInitializer<ReleaseCentre> RELEASE_CENTRE_EXTENSION_INITIALIZER = new LazyInitializer<ReleaseCentre>() {
		@Override
		public void initializeLazyRelationships(ReleaseCentre entity) {
			Hibernate.initialize(entity.getExtensions());
		}
	};

	@RequestMapping("/centres")
	@ResponseBody
	public List<Map<String, Object>> getReleaseCentres(HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<ReleaseCentre> centres = releaseCentreService.findAll(authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(centres, request, RELEASE_CENTRE_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}")
	@ResponseBody
	public Map getReleaseCentre(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		ReleaseCentre centre = releaseCentreService.find(releaseCentreBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(centre, request, RELEASE_CENTRE_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions")
	@ResponseBody
	public List<Map<String, Object>> getExtensions(@PathVariable String releaseCentreBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<Extension> extensions = releaseCentreService.find(releaseCentreBusinessKey, RELEASE_CENTRE_EXTENSION_INITIALIZER, authenticatedId).getExtensions();
		return hypermediaGenerator.getEntityCollectionHypermedia(extensions, request, EXTENSION_LINKS);
	}

}
