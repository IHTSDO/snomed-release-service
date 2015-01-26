package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/login")
@Api(value = "Login", position = 7)
public class LoginController {

	@Autowired
	private AuthenticationService authenticationService;

	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation( value = "Returns an authentication token",
		notes = "Returns an autentication token which should be set in the headers of future requests " )
	@ResponseBody
	public ResponseEntity<Map<String,String>> login(@RequestParam String username, @RequestParam String password) {
		String authenticationToken = authenticationService.authenticate(username, password);
		Map<String, String> response = new HashMap<>();
		if (authenticationToken != null) {
			response.put("authenticationToken", authenticationToken);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} else {
			response.put(ControllerConstants.ERROR_MESSAGE, "Username or password are incorrect.");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
	}

}
