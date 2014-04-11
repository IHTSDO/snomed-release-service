package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/login")
public class LoginController {

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
		String authenticationToken = authenticationService.authenticate(username, password);
		if (authenticationToken != null) {
			HashMap<String, String> response = new HashMap<>();
			response.put("authenticationToken", authenticationToken);
			return hypermediaGenerator.getEntityHypermedia(response, request);
		} else {
			throw new AuthenticationCredentialsNotFoundException("Username or password are incorrect.");
		}
	}

}
