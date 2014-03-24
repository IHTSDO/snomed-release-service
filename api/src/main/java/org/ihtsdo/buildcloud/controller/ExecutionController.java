package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.controller.helper.LinkPath;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletOutputStream;
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
	private HypermediaGenerator hypermediaGenerator;

	static final  LinkPath [] EXECUTION_LINKS = { new LinkPath ("configuration"), 
												  new LinkPath ("buildScripts|build-scripts.zip") };

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createExecution(@PathVariable String buildCompositeKey,
											   HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.create(buildCompositeKey, authenticatedId);

		return hypermediaGenerator.getEntityHypermediaJustCreated(execution, request, EXECUTION_LINKS);
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
	public Map<String, Object> find(@PathVariable String buildCompositeKey, @PathVariable String executionId,
									HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.find(buildCompositeKey, executionId, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping(value = "/{executionId}/configuration", produces="application/json")
	@ResponseBody
	public void getConfiguration(@PathVariable String buildCompositeKey, @PathVariable String executionId,
								 HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		String executionConfiguration = executionService.loadConfiguration(buildCompositeKey, executionId, authenticatedId);
		response.setContentType("application/json");
		response.getOutputStream().print(executionConfiguration);
	}

	@RequestMapping(value = "/{executionId}/trigger", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> triggerBuild(@PathVariable String buildCompositeKey, @PathVariable String executionId,
											HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		Execution execution = executionService.triggerBuild(buildCompositeKey, executionId, authenticatedId);
		return hypermediaGenerator.getEntityHypermediaOfAction(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping("/{executionId}/build-scripts.zip")
	@ResponseBody
	public void getBuildScrips(@PathVariable String buildCompositeKey, @PathVariable String executionId,
							   HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		response.setContentType("application/zip");
		ServletOutputStream outputStream = response.getOutputStream();
		executionService.streamBuildScriptsZip(buildCompositeKey, executionId, authenticatedId, outputStream);
	}

	@RequestMapping(value = "/{executionId}/output/**", method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
	@ResponseBody
	public void uploadOutputFile(@PathVariable String buildCompositeKey, @PathVariable String executionId,
								 HttpServletRequest request,
								 HttpServletResponse response) throws IOException {

		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String filePath = path.substring(path.indexOf("/output/") + 8);

		String authenticatedId = SecurityHelper.getSubject();
		Long contentLength = asLong(request.getHeader("content-length"));
		if (contentLength != null) {
			executionService.saveOutputFile(buildCompositeKey, executionId, filePath,
					request.getInputStream(), contentLength, authenticatedId);
			response.setStatus(HttpServletResponse.SC_CREATED);
		} else {
			// Ask the client for content length so we may stream to permanent storage.
			response.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);
		}
	}
	private Long asLong(String longString) {
		if (longString != null && longString.matches("\\d+")) {
			return Long.parseLong(longString);
		} else {
			return null;
		}
	}

}
