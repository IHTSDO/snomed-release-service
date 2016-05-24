package org.ihtsdo.buildcloud.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.QATestConfig;
import org.ihtsdo.buildcloud.service.BuildService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}/builds")
@Api(value = "Build", position = 1)
public class BuildController {

	private final Logger logger = LoggerFactory.getLogger(BuildController.class);

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private PublishService publishService;

	private static final String[] BUILD_LINKS = {"configuration","qaTestConfig", "inputfiles", "outputfiles","buildReport","logs" };

	@RequestMapping( method = RequestMethod.POST )
	@ApiOperation( value = "Create a build",
		notes = "Create a build for given product key and release center key and returns build id" )
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws BusinessServiceException {

		final Build build = buildService.createBuildFromProduct(releaseCenterKey, productKey);

		final boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS), HttpStatus.CREATED);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET )
	@ApiOperation( value = "Returns a list all builds for a logged in user",
		notes = "Returns a list all builds visible to the currently logged in user, "
			+ "so this could potentially span across Release Centres" )
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws ResourceNotFoundException {
		final List<Build> builds = buildService.findAllDesc(releaseCenterKey, productKey);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}", method = RequestMethod.GET )
	@ResponseBody
	@ApiOperation( value = "Get a build id",
	notes = "Returns a single build object for given key" )
	public Map<String, Object> getBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId);

		ifBuildIsNullThrow(productKey, buildId, build);

		final boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}/configuration", produces = "application/json", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves configuration details",
		notes = "Retrieves configuration details for given product key, release center key, and build id" )
	public Map<String, Object> getConfiguration(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			final HttpServletRequest request) throws IOException, BusinessServiceException {
		final BuildConfiguration buildConfiguration = buildService.loadBuildConfiguration(releaseCenterKey, productKey, buildId);
		final Map<String,Object> result = new HashMap<>();
		if (buildConfiguration != null ) {
			result.putAll(hypermediaGenerator.getEntityHypermedia(buildConfiguration, true, request));
		}
		return result;
	}
	
	
	@RequestMapping(value = "/{buildId}/qaTestConfig", produces = "application/json", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves QA test configuration details",
		notes = "Retrieves configuration details for given product key, release center key, and build id" )
	public Map<String, Object> getQqTestConfig(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			final HttpServletRequest request) throws IOException, BusinessServiceException {
		final Map<String,Object> result = new HashMap<>();
		final QATestConfig qaTestConfig = buildService.loadQATestConfig(releaseCenterKey, productKey, buildId);
		if( qaTestConfig != null) {
			result.putAll(hypermediaGenerator.getEntityHypermedia(qaTestConfig, true, request));
		}
		return result;
	}
	
	
	@RequestMapping(value = "/{buildId}/buildReport", produces = "application/json", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves build report details",
		notes = "Retrieves buildReport details for given product key, release center key, and build id" )
	public void getBuildReport(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			final HttpServletRequest request, final HttpServletResponse response) throws IOException, BusinessServiceException {
		
		try (InputStream outputFileStream = buildService.getBuildReportFile(releaseCenterKey, productKey, buildId)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No build_report json file found for build: " + productKey + "/" + buildId + "/");
			}
		}
	}

	@RequestMapping(value = "/{buildId}/inputfiles", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves list of input file names",
		notes = "Retrieves list of input file names for given release center, product key and build id" )
	public List<Map<String, Object>> listPackageInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws IOException, ResourceNotFoundException {

		final List<String> relativeFilePaths = buildService.getInputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{buildId}/inputfiles/{inputFileName:.*}", method = RequestMethod.GET)
	@ApiOperation( value = "Download a specific file",
		notes = "Download a specific file content for given release center, product key, build id and given input file name combination" )
	public void getPackageInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String inputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getInputFile(releaseCenterKey, productKey, buildId, inputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{buildId}/outputfiles", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves a list of file names from output directory",
		notes = "Retrieves a list of file names from output directory for given release center, "
				+ "product key, build id combination" )
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws BusinessServiceException {

		final List<String> relativeFilePaths = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@RequestMapping(value = "/{buildId}/outputfiles/{outputFileName:.*}", method = RequestMethod.GET)
	@ApiOperation( value = "Download a specific file from output directory",
		notes = "Download a specific file from output directory for given release center, "
			+ "product key, build id and file name combination" )
	public void getPackageOutputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String outputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getOutputFile(releaseCenterKey, productKey, buildId, outputFileName)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@RequestMapping(value = "/{buildId}/trigger", method = RequestMethod.POST)
	@ResponseBody
	@ApiIgnore
	public Map<String, Object> triggerProduct(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, @RequestParam(value = "failureExportMax", required = false) final Integer failureExportMax, final HttpServletRequest request) throws BusinessServiceException {
		//when failureExportMax is set to less than zero means exporting all results. The default value is 10 when not set
		final Build build = buildService.triggerBuild(releaseCenterKey, productKey, buildId, failureExportMax);

		return hypermediaGenerator.getEntityHypermediaOfAction(build, request, BUILD_LINKS);
	}

	@RequestMapping(value = "/{buildId}/publish", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation( value = "Publish a release for given build id",
	notes = "Publish release for given build id to make it available in repository for wider usages" )
	public void publishBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId) throws BusinessServiceException {

		final Build build = buildService.find(releaseCenterKey, productKey, buildId);
		ifBuildIsNullThrow(productKey, buildId, build);
		publishService.publishBuild(build, true);
	}

	@RequestMapping(value = "/{buildId}/logs" , method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves a list of build log file names",
		notes = "Retrieves a list of build log file names for given release center, product key, and build id" )
	public List<Map<String, Object>> getBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {

		return convertFileListToEntities(request, buildService.getLogFilePaths(releaseCenterKey, productKey, buildId));
	}

	@RequestMapping(value = "/{buildId}/logs/{logFileName:.*}", method = RequestMethod.GET)
	@ApiOperation( value = "Download a specific build log file",
		notes = "Download a specific log file for given release center, "
		+ "product key, build id and file name combination" )
	public void getBuildLog(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String logFileName, final HttpServletResponse response) throws ResourceNotFoundException, IOException {

		try (InputStream outputFileStream = buildService.getLogFile(releaseCenterKey, productKey, buildId, logFileName)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("Did not find requested log file: " + productKey + "/" + buildId + "/" + logFileName);
			}
		}
	}

	@RequestMapping(value = "/{buildId}/logs/{logFileName:.*}", method = RequestMethod.HEAD)
	@ApiOperation(value = "Download a specific build log file", notes = "Download a specific log file for given release center, "
			+ "product key, build id and file name combination")
	public void getBuildLogHead(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, @PathVariable final String logFileName, final HttpServletResponse response)
			throws ResourceNotFoundException, IOException {

		try (InputStream outputFileStream = buildService.getLogFile(releaseCenterKey, productKey, buildId, logFileName)) {
			// This will blow up with 404 if the file isn't found. HTTP HEAD demands that no body is returned, so nothing to do here
			logger.debug("HTTP 200 response to head request for {}/{}/{}", productKey, buildId, logFileName);
		}
	}

	private void ifBuildIsNullThrow(final String productKey, final String buildId, final Build build) throws ResourceNotFoundException {
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build, productKey: " + productKey + ", buildId:" + buildId);
		}
	}

	private List<Map<String, Object>> convertFileListToEntities(final HttpServletRequest request, final List<String> relativeFilePaths) {
		final List<Map<String, String>> files = new ArrayList<>();
		for (final String relativeFilePath : relativeFilePaths) {
			final Map<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, relativeFilePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

}
