package org.ihtsdo.buildcloud.controller;

import org.hibernate.Hibernate;
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

import java.util.List;
import java.util.Set;

@Controller
public class ReleaseCentreController {

	@Autowired
	private ReleaseCentreService releaseCentreService;

	private static final LazyInitializer<ReleaseCentre> RELEASE_CENTRE_EXTENSION_INITIALIZER = new LazyInitializer<ReleaseCentre>() {
		@Override
		public void initializeLazyRelationships(ReleaseCentre entity) {
			Hibernate.initialize(entity.getExtensions());
		}
	};

	@RequestMapping("/centres")
	public @ResponseBody List<ReleaseCentre> getReleaseCentres() {
		String authenticatedId = SecurityHelper.getSubject();
		return releaseCentreService.findAll(authenticatedId);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}")
	public @ResponseBody ReleaseCentre getReleaseCentre(@PathVariable String releaseCentreBusinessKey) {
		String authenticatedId = SecurityHelper.getSubject();
		return releaseCentreService.find(releaseCentreBusinessKey, authenticatedId);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions")
	public @ResponseBody Set<Extension> getExtensions(@PathVariable String releaseCentreBusinessKey) {
		String authenticatedId = SecurityHelper.getSubject();
		return releaseCentreService.find(releaseCentreBusinessKey, RELEASE_CENTRE_EXTENSION_INITIALIZER, authenticatedId).getExtensions();
	}

}
