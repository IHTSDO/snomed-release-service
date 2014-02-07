package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products/{productBusinessKey}/builds")
public class ProductBuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCentreBusinessKey,
											   @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey,
											   HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Build> builds = buildService.findForProduct(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BuildController.BUILD_LINKS, "/builds");
	}

}
