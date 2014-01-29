package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Release;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ReleaseController {

	@Autowired
	private ReleaseService releaseService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] RELEASE_LINKS = {"packages"};

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/releases")
	@ResponseBody
	public List<Map<String, Object>> getReleases(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
												 @PathVariable String productBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<Release> releases = releaseService.findAll(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(releases, request, RELEASE_LINKS);
	}

	@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/releases/{releaseBusinessKey}")
	@ResponseBody
	public Map getExtension(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey,
							@PathVariable String productBusinessKey, @PathVariable String releaseBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Release release = releaseService.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, releaseBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(release, request, RELEASE_LINKS);
	}

}
