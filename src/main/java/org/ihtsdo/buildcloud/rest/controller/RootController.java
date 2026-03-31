package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
@Hidden
public class RootController {

	private final HypermediaGenerator hypermediaGenerator;

	private static final String[] ROOT_LINK = {"centers", "user", "login"};

	@Autowired
	public RootController(HypermediaGenerator hypermediaGenerator) {
		this.hypermediaGenerator = hypermediaGenerator;
	}

	@RequestMapping
	public Map<String, Object> getRoot(HttpServletRequest request) {
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(new HashMap<String, String>(), currentResource, request, ROOT_LINK);
	}

}
