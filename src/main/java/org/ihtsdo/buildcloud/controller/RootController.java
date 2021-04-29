package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
@ApiIgnore
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
