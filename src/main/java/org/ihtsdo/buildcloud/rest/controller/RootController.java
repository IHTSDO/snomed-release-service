package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
@Hidden
public class RootController {

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] ROOT_LINK = {"centers", "user", "login"};

	@RequestMapping
	@ResponseBody
	public Map<String, Object> getRoot(HttpServletRequest request) {
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(new HashMap<String, String>(), currentResource, request, ROOT_LINK);
	}

}
