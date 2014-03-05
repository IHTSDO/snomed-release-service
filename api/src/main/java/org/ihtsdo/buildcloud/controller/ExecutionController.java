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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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

	static final String[] EXECUTION_LINKS = { "configuration" };

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createExecution(@PathVariable String buildCompositeKey, HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.create(buildCompositeKey, authenticatedId);

		return hypermediaGenerator.getEntityHypermedia(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> findAll(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Execution> executions = executionService.findAll(buildCompositeKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(executions, request, EXECUTION_LINKS);
	}

	@RequestMapping("/{executionId}")
	@ResponseBody
	public Map<String, Object> find(@PathVariable String buildCompositeKey, @PathVariable String executionId, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.find(buildCompositeKey, executionId, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping(value = "/{executionId}/configuration", produces="application/json")
	@ResponseBody
	public void getConfiguration(@PathVariable String buildCompositeKey, @PathVariable String executionId, HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		String executionConfiguration = executionService.loadConfiguration(buildCompositeKey, executionId, authenticatedId);
		response.setContentType("application/json");
		response.getOutputStream().print(executionConfiguration);
	}

}
