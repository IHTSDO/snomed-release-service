package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.ExecutionService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.EntityAlreadyExistsException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/builds/{buildKey}/executions")
public class ExecutionController {

	@Autowired
	private ExecutionService executionService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private PublishService publishService;

	private static final String[] EXECUTION_LINKS = {"configuration", "inputfiles", "outputfiles", "logs"};

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createExecution(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			HttpServletRequest request) throws BusinessServiceException {

		Execution execution = executionService.createExecutionFromBuild(releaseCenterKey, buildKey);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(execution, currentResource, request, EXECUTION_LINKS), HttpStatus.CREATED);
	}

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getExecutions(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			HttpServletRequest request) throws ResourceNotFoundException {
		List<Execution> executions = executionService.findAllDesc(releaseCenterKey, buildKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(executions, request, EXECUTION_LINKS);
	}

	@RequestMapping("/{executionId}")
	@ResponseBody
	public Map<String, Object> getExecution(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@PathVariable String executionId, HttpServletRequest request) throws ResourceNotFoundException {
		Execution execution = executionService.find(releaseCenterKey, buildKey, executionId);

		ifExecutionIsNullThrow(buildKey, executionId, execution);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(execution, currentResource, request, EXECUTION_LINKS);
	}

	@RequestMapping(value = "/{executionId}/configuration", produces = "application/json")
	@ResponseBody
	public void getConfiguration(@PathVariable String releaseCenterKey, @PathVariable String buildKey, @PathVariable String executionId,
			HttpServletResponse response) throws IOException, BusinessServiceException {

		String executionConfiguration = executionService.loadConfiguration(releaseCenterKey, buildKey, executionId);
		response.setContentType("application/json");
		response.getOutputStream().print(executionConfiguration);
	}

	@RequestMapping(value = "/{executionId}/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> listPackageInputFiles(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@PathVariable String executionId, HttpServletRequest request) throws IOException, ResourceNotFoundException {

		List<String> relativeFilePaths = executionService.getInputFilePaths(releaseCenterKey, buildKey, executionId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{executionId}/inputfiles/{inputFileName:.*}")
	public void getPackageInputFile(@PathVariable String releaseCenterKey, @PathVariable String buildKey, @PathVariable String executionId,
			@PathVariable String inputFileName, HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = executionService.getInputFile(releaseCenterKey, buildKey, executionId, inputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{executionId}/outputfiles")
	@ResponseBody
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@PathVariable String executionId, HttpServletRequest request) throws BusinessServiceException {

		List<String> relativeFilePaths = executionService.getOutputFilePaths(releaseCenterKey, buildKey, executionId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{executionId}/outputfiles/{outputFileName:.*}")
	public void getPackageOutputFile(@PathVariable String releaseCenterKey, @PathVariable String buildKey, @PathVariable String executionId,
			@PathVariable String outputFileName, HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = executionService.getOutputFile(releaseCenterKey, buildKey, executionId, outputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{executionId}/trigger", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> triggerBuild(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@PathVariable String executionId, HttpServletRequest request) throws BusinessServiceException {
		Execution execution = executionService.triggerExecution(releaseCenterKey, buildKey, executionId);

		return hypermediaGenerator.getEntityHypermediaOfAction(execution, request, EXECUTION_LINKS);
	}

	@RequestMapping(value = "/{executionId}/output/publish")
	@ResponseBody
	public void publishReleasePackage(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
 @PathVariable String executionId)
			throws BusinessServiceException, EntityAlreadyExistsException {

		Execution execution = executionService.find(releaseCenterKey, buildKey, executionId);
		ifExecutionIsNullThrow(buildKey, executionId, execution);
		publishService.publishExecution(execution);
	}

	@RequestMapping(value = "/{executionId}/logs")
	@ResponseBody
	public List<Map<String, Object>> getExecutionLogs(@PathVariable String releaseCenterKey, @PathVariable String buildKey,
			@PathVariable String executionId, HttpServletRequest request) throws ResourceNotFoundException {

		return convertFileListToEntities(request, executionService.getLogFilePaths(releaseCenterKey, buildKey, executionId));
	}

	@RequestMapping(value = "/{executionId}/logs/{logFileName:.*}")
	public void getExecutionLog(@PathVariable String releaseCenterKey, @PathVariable String buildKey, @PathVariable String executionId,
			@PathVariable String logFileName, HttpServletResponse response) throws ResourceNotFoundException, IOException {

		try (InputStream outputFileStream = executionService.getLogFile(releaseCenterKey, buildKey, executionId, logFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	private void ifExecutionIsNullThrow(String buildKey, String executionId, Execution execution) throws ResourceNotFoundException {
		if (execution == null) {
			throw new ResourceNotFoundException("Unable to find execution, buildKey: " + buildKey + ", executionId:" + executionId);
		}
	}

	private List<Map<String, Object>> convertFileListToEntities(HttpServletRequest request, List<String> relativeFilePaths) {
		List<Map<String, String>> files = new ArrayList<>();
		for (String relativeFilePath : relativeFilePaths) {
			Map<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, relativeFilePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

}
