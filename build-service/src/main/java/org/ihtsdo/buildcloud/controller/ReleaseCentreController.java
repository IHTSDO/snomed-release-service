package org.ihtsdo.buildcloud.controller;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.security.SecurityUtils;
import org.ihtsdo.buildcloud.service.helper.LazyInitializer;
import org.ihtsdo.buildcloud.service.ReleaseCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

@Controller
public class ReleaseCentreController {

	@Autowired
	private ReleaseCentreService releaseCentreService;

	private SecurityUtils securityUtils;

	private static final LazyInitializer<ReleaseCentre> RELEASE_CENTRE_EXTENSION_INITIALIZER = new LazyInitializer<ReleaseCentre>() {
		@Override
		public void initializeLazyRelationships(ReleaseCentre entity) {
			Hibernate.initialize(entity.getExtensions());
		}
	};

	public ReleaseCentreController() {
		securityUtils = new SecurityUtils();
	}

	@RequestMapping("/release-centres")
	public @ResponseBody List<ReleaseCentre> getReleaseCentres(@ModelAttribute("authenticatedId") String authenticatedId) {
		return releaseCentreService.findAll(authenticatedId);
	}

	@RequestMapping("/release-centres/{releaseCentreBusinessKey}")
	public @ResponseBody ReleaseCentre getReleaseCentre(@PathVariable String releaseCentreBusinessKey, @ModelAttribute("authenticatedId") String authenticatedId) {
		return releaseCentreService.find(releaseCentreBusinessKey, authenticatedId);
	}

	@RequestMapping("/release-centres/{releaseCentreBusinessKey}/extensions")
	public @ResponseBody Set<Extension> getExtensions(@PathVariable String releaseCentreBusinessKey, @ModelAttribute("authenticatedId") String authenticatedId) {
		return releaseCentreService.find(releaseCentreBusinessKey, RELEASE_CENTRE_EXTENSION_INITIALIZER, authenticatedId).getExtensions();
	}

	@ModelAttribute("authenticatedId")
	public String getAuthenticatedId(HttpServletRequest request) {
		return securityUtils.getAuthenticatedId(request);
	}

}
