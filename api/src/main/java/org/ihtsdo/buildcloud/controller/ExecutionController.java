package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/executions")
public class ExecutionController {

	@Autowired
	private ExecutionService executionService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	static final String[] EXECUTION_LINKS = {};

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createExecution(@PathVariable String buildCompositeKey, HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.create(buildCompositeKey, authenticatedId);

		return hypermediaGenerator.getEntityHypermedia(execution, request, EXECUTION_LINKS);
	}

}
