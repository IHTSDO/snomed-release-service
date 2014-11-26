package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}/builds")
public class BuildController {

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private PublishService publishService;

	private static final String[] BUILD_LINKS = {"configuration", "inputfiles", "outputfiles", "logs"};

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			HttpServletRequest request) throws BusinessServiceException {

		Build build = buildService.createBuildFromProduct(releaseCenterKey, productKey);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS), HttpStatus.CREATED);
	}

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			HttpServletRequest request) throws ResourceNotFoundException {
		List<Build> builds = buildService.findAllDesc(releaseCenterKey, productKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping("/{buildId}")
	@ResponseBody
	public Map<String, Object> getBuild(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId, HttpServletRequest request) throws ResourceNotFoundException {
		Build build = buildService.find(releaseCenterKey, productKey, buildId);

		ifBuildIsNullThrow(productKey, buildId, build);

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}/configuration", produces = "application/json")
	@ResponseBody
	public Map<String, Object> getConfiguration(@PathVariable String releaseCenterKey, @PathVariable String productKey, @PathVariable String buildId,
			HttpServletRequest request) throws IOException, BusinessServiceException {

		BuildConfiguration buildConfiguration = buildService.loadConfiguration(releaseCenterKey, productKey, buildId);
		return hypermediaGenerator.getEntityHypermedia(buildConfiguration, true, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> listPackageInputFiles(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId, HttpServletRequest request) throws IOException, ResourceNotFoundException {

		List<String> relativeFilePaths = buildService.getInputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{buildId}/inputfiles/{inputFileName:.*}")
	public void getPackageInputFile(@PathVariable String releaseCenterKey, @PathVariable String productKey, @PathVariable String buildId,
			@PathVariable String inputFileName, HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getInputFile(releaseCenterKey, productKey, buildId, inputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{buildId}/outputfiles")
	@ResponseBody
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId, HttpServletRequest request) throws BusinessServiceException {

		List<String> relativeFilePaths = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{buildId}/outputfiles/{outputFileName:.*}")
	public void getPackageOutputFile(@PathVariable String releaseCenterKey, @PathVariable String productKey, @PathVariable String buildId,
			@PathVariable String outputFileName, HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getOutputFile(releaseCenterKey, productKey, buildId, outputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{buildId}/trigger", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> triggerProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId, HttpServletRequest request) throws BusinessServiceException {
		Build build = buildService.triggerBuild(releaseCenterKey, productKey, buildId);

		return hypermediaGenerator.getEntityHypermediaOfAction(build, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}/output/publish")
	@ResponseBody
	public void publishReleasePackage(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId) throws BusinessServiceException {

		Build build = buildService.find(releaseCenterKey, productKey, buildId);
		ifBuildIsNullThrow(productKey, buildId, build);
		publishService.publishBuild(build);
	}

	@RequestMapping(value = "/{buildId}/logs")
	@ResponseBody
	public List<Map<String, Object>> getBuildLogs(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@PathVariable String buildId, HttpServletRequest request) throws ResourceNotFoundException {

		return convertFileListToEntities(request, buildService.getLogFilePaths(releaseCenterKey, productKey, buildId));
	}

	@RequestMapping(value = "/{buildId}/logs/{logFileName:.*}")
	public void getBuildLog(@PathVariable String releaseCenterKey, @PathVariable String productKey, @PathVariable String buildId,
			@PathVariable String logFileName, HttpServletResponse response) throws ResourceNotFoundException, IOException {

		try (InputStream outputFileStream = buildService.getLogFile(releaseCenterKey, productKey, buildId, logFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	private void ifBuildIsNullThrow(String productKey, String buildId, Build build) throws ResourceNotFoundException {
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build, productKey: " + productKey + ", buildId:" + buildId);
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
