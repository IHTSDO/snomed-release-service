package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ExecutionService;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/executions")
public class ExecutionController {

	@Autowired
	private ExecutionService executionService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	static final String[] EXECUTION_LINKS = { "configuration", "buildScripts|build-scripts.zip" };

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createExecution(@PathVariable String buildCompositeKey,
											   HttpServletRequest request) throws IOException, BadConfigurationException {
		User authenticatedUser = SecurityHelper.getSubject();
		Execution execution = executionService.create(buildCompositeKey, authenticatedUser);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(execution, currentResource, request, EXECUTION_LINKS);
	}

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> findAll(@PathVariable String buildCompositeKey, HttpServletRequest request) {
		User authenticatedUser = SecurityHelper.getSubject();
		List<Execution> executions = executionService.findAll(buildCompositeKey, authenticatedUser);
		return hypermediaGenerator.getEntityCollectionHypermedia(executions, request, EXECUTION_LINKS);
	}

	@RequestMapping("/{executionId}")
	@ResponseBody
	public Map<String, Object> find(@PathVariable String buildCompositeKey, @PathVariable String executionId,
									HttpServletRequest request) {
		User authenticatedUser = SecurityHelper.getSubject();
		Execution execution = executionService.find(buildCompositeKey, executionId, authenticatedUser);
		
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(execution, currentResource, request, EXECUTION_LINKS);
	}

	@RequestMapping(value = "/{executionId}/configuration", produces="application/json")
	@ResponseBody
	public void getConfiguration(@PathVariable String buildCompositeKey, @PathVariable String executionId,
								 HttpServletResponse response) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		String executionConfiguration = executionService.loadConfiguration(buildCompositeKey, executionId, authenticatedUser);
		response.setContentType("application/json");
		response.getOutputStream().print(executionConfiguration);
	}

	@RequestMapping(value = "/{executionId}/trigger", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> triggerBuild(@PathVariable String buildCompositeKey, @PathVariable String executionId,
											HttpServletRequest request) throws Exception {
		User authenticatedUser = SecurityHelper.getSubject();
		Execution execution = executionService.triggerBuild(buildCompositeKey, executionId, authenticatedUser);
		return hypermediaGenerator.getEntityHypermediaOfAction(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping("/{executionId}/build-scripts.zip")
	@ResponseBody
	public void getBuildScrips(@PathVariable String buildCompositeKey, @PathVariable String executionId,
							   HttpServletResponse response) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		response.setContentType("application/zip");
		ServletOutputStream outputStream = response.getOutputStream();
		executionService.streamBuildScriptsZip(buildCompositeKey, executionId, authenticatedUser, outputStream);
	}

	@RequestMapping(value = "/{executionId}/output/**")
	@ResponseBody
	public void downloadOutputFile(@PathVariable String buildCompositeKey, @PathVariable String executionId,
								   HttpServletRequest request,
								   HttpServletResponse response) throws IOException {

		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String filePath = path.substring(path.indexOf("/output/") + 8);

		User authenticatedUser = SecurityHelper.getSubject();
		ServletOutputStream outputStream = response.getOutputStream();
		InputStream outputFileInputStream = executionService.getOutputFile(buildCompositeKey, executionId, filePath, authenticatedUser);
		if (outputFileInputStream != null) {
			try {
				StreamUtils.copy(outputFileInputStream, outputStream);
			} finally {
				outputFileInputStream.close();
			}
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@RequestMapping(value = "/{executionId}/status/{status}", method = RequestMethod.POST)
	@ResponseBody
	public void setStatus(@PathVariable String buildCompositeKey, @PathVariable String executionId, @PathVariable String status) {
		User authenticatedUser = SecurityHelper.getSubject();
		executionService.updateStatus(buildCompositeKey, executionId, status, authenticatedUser);
	}

	private Long asLong(String longString) {
		if (longString != null && longString.matches("\\d+")) {
			return Long.parseLong(longString);
		} else {
			return null;
		}
	}

}
