package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static final String[] BUILD_LINKS = {"configuration","qaTestConfig", "inputfiles","inputGatherReport", "inputPrepareReport", "outputfiles", "buildReport", "logs", "buildLogs", "preConditionCheckReports", "postConditionCheckReports", "classificationResultsOutputFiles"};

	@PostMapping
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Create a build",
		notes = "Create a build for given product key and release center key and returns build id" )
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws BusinessServiceException {
		final String currentUser = SecurityUtil.getUsername();
		final Build build = buildService.createBuildFromProduct(releaseCenterKey, productKey, null, currentUser, null, null, null, null, null);

		final boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS), HttpStatus.CREATED);
	}

	@DeleteMapping(value = "/{buildId}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Delete a build",
			notes = "" +
					"Delete a build for given product key and release center key and build id" )
	@ResponseBody
	public ResponseEntity<Map<String, Object>> deleteBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
														   @PathVariable final String buildId, final HttpServletRequest request) throws BusinessServiceException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);

		ifBuildIsNullThrow(productKey, buildId, build);

		if (!CollectionUtils.isEmpty(build.getTags()) && build.getTags().contains(Build.Tag.PUBLISHED)) {
			throw new BusinessServiceException("You can not delete the PUBLISHED build");
		}

		buildService.delete(releaseCenterKey, productKey, buildId);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a list all builds for a logged in user",
		notes = "Returns a list all builds visible to the currently logged in user, "
			+ "so this could potentially span across Release Centres" )
	@ResponseBody
	public List<Map<String, Object>> getBuilds(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											   @RequestParam(required = false) boolean includeBuildConfiguration,
											   @RequestParam(required = false) boolean includeQAConfiguration,
			final HttpServletRequest request) throws ResourceNotFoundException {
		final List<Build> builds = buildService.findAllDesc(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, true, true);
		return hypermediaGenerator.getEntityCollectionHypermedia(builds, request, BUILD_LINKS);
	}

	@GetMapping(value = "/{buildId}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Get a build id",
	notes = "Returns a single build object for given key" )
	public Map<String, Object> getBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, true, true, true, true);

		ifBuildIsNullThrow(productKey, buildId, build);

		final boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(build, currentResource, request, BUILD_LINKS);
	}

	@GetMapping(value = "/{buildId}/configuration", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
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
	
	
	@GetMapping(value = "/{buildId}/qaTestConfig", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
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
	
	
	@GetMapping(value = "/{buildId}/buildReport", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
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

	@GetMapping(value = "/{buildId}/inputfiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves list of input file names",
		notes = "Retrieves list of input file names for given release center, product key and build id" )
	public List<Map<String, Object>> listPackageInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws IOException, ResourceNotFoundException {

		final List<String> relativeFilePaths = buildService.getInputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}
	
	@GetMapping(value = "/{buildId}/inputPrepareReport")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves the report for preparing input source files.",
		notes = "Product key and build id are required. And the report might not exist if no preparation is required." )
	public void getInputPrepareReport(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getBuildInputFilesPrepareReport(releaseCenterKey, productKey, buildId)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No input file prepare report json file found for build: " + productKey + "/" + buildId + "/");
			}
		}
	}

	@GetMapping(value = "/{buildId}/inputGatherReport")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves the report for gathering input source files.",
			notes = "Product key and build id are required. And the report might not exist if no preparation is required." )
	public void getInputGatherReport(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
									  @PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getBuildInputGatherReport(releaseCenterKey, productKey, buildId)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No input file gather report json file found for build: " + productKey + "/" + buildId + "/");
			}
		}
	}


	@GetMapping(value = "/{buildId}/inputfiles/{inputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Download a specific file",
		notes = "Download a specific file content for given release center, product key, build id and given input file name combination" )
	public void getPackageInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String inputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getInputFile(releaseCenterKey, productKey, buildId, inputFileName)) {
			response.setContentType("text/plain; charset=utf-8");
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@GetMapping(value = "/{buildId}/outputfiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Retrieves a list of file names from output directory",
		notes = "Retrieves a list of file names from output directory for given release center, "
				+ "product key, build id combination" )
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws BusinessServiceException {

		final List<String> relativeFilePaths = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@GetMapping(value = "/{buildId}/outputfiles/{outputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Download a specific file from output directory",
		notes = "Download a specific file from output directory for given release center, "
			+ "product key, build id and file name combination" )
	public void getPackageOutputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String outputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getOutputFile(releaseCenterKey, productKey, buildId, outputFileName)) {
			response.setContentType("text/plain; charset=utf-8");
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@PostMapping(value = "/{buildId}/trigger")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiIgnore
	public Map<String, Object> triggerProduct(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											  @PathVariable final String buildId, @RequestParam(value = "failureExportMax", required = false) final Integer failureExportMax,
											  @RequestParam(value = "mrcmValidationForm", required = false, defaultValue = "stated") final QATestConfig.CharacteristicType mrcmValidationForm, HttpServletRequest request) throws BusinessServiceException {
		//when failureExportMax is set to less than zero means exporting all results. The default value is 10 when not set
		final Build build = buildService.triggerBuild(releaseCenterKey, productKey, buildId, failureExportMax, mrcmValidationForm,true);
		return hypermediaGenerator.getEntityHypermediaOfAction(build, request, BUILD_LINKS);
	}

	@PostMapping(value = "/{buildId}/visibility")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Update visibility for build", notes = "Update an existing build with the visibility flag")
	public ResponseEntity updateVisibility(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										   @PathVariable final String buildId, @RequestParam(required = true, defaultValue = "true") boolean visibility) {
		buildService.updateVisibility(releaseCenterKey, productKey, buildId, visibility);
		return new ResponseEntity(HttpStatus.OK);
	}

	@PostMapping(value = "/{buildId}/tags")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Update tags for build", notes = "Update an existing build with the tags")
	public ResponseEntity updateTags(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										   @PathVariable final String buildId, @RequestParam(required = true) List<Build.Tag> tags) {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);
		buildService.saveTags(build, tags);
		return new ResponseEntity(HttpStatus.OK);
	}

	@PostMapping(value = "/{buildId}/publish")
	@IsAuthenticatedAsAdminOrReleaseManager
	@ResponseBody
	@ApiOperation( value = "Publish a release for given build id",
	notes = "Publish release for given build id to make it available in repository for wider usages" )
	public void publishBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, @RequestParam(required = false) String environment) throws BusinessServiceException {

		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);
		ifBuildIsNullThrow(productKey, buildId, build);
		publishService.publishBuildAsync(build, true, environment);
	}

	@GetMapping(value = "/{buildId}/publish/status")
	@IsAuthenticatedAsAdminOrReleaseManager
	@ResponseBody
	@ApiOperation( value = "Get publishing release status",
			notes = "Get publishing release status for given build id")
	public ResponseEntity<ProcessingStatus> getPublishingBuildStatus(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
																	 @PathVariable final String buildId) {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);
		ifBuildIsNullThrow(productKey, buildId, build);
		ProcessingStatus status = publishService.getPublishingBuildStatus(build);
		if (status != null) {
			return new ResponseEntity<>(status, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(value = "/{buildId}/logs")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Retrieves a list of build log file names",
		notes = "Retrieves a list of build log file names for given release center, product key, and build id" )
	public List<Map<String, Object>> getBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {

		return convertFileListToEntities(request, buildService.getLogFilePaths(releaseCenterKey, productKey, buildId));
	}

	@GetMapping(value = "/{buildId}/logs/{logFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
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
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
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

	@GetMapping(value = "/{buildId}/preConditionCheckReports")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves Pre-Condition Check Report",
			notes = "Retrieves configuration details for given product key, release center key, and build id" )
	public void getPreConditionCheckReports(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
	                                  @PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getPreConditionChecksReport(releaseCenterKey, productKey, buildId)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No input file prepare report json file found for build: " + productKey + "/" + buildId + "/");
			}
		}
	}

	@GetMapping(value = "/{buildId}/postConditionCheckReports")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves Post-Condition Check Report",
			notes = "Retrieves configuration details for given product key, release center key, and build id" )
	public void getPostConditionCheckReports(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											@PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getPostConditionChecksReport(releaseCenterKey, productKey, buildId)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No input file prepare report json file found for build: " + productKey + "/" + buildId + "/");
			}
		}
	}

	@GetMapping(value = "/{buildId}/classificationResultsOutputFiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@ApiOperation( value = "Retrieves classification results for output files",
			notes = "Retrieves configuration details for given product key, release center key, and build id" )
	public List<Map<String, Object>> getClassificationResultsOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											 @PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {
		final List<String> relativeFilePaths = buildService.getClassificationResultOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@GetMapping(value = "/{buildId}/classificationResultsOutputFiles/{inputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Download a specific file",
			notes = "Download a specific file content for given release center, product key, build id and given file name combination" )
	public void getClassificationResultsOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
									@PathVariable final String inputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getClassificationResultOutputFile(releaseCenterKey, productKey, buildId, inputFileName)) {
			response.setContentType("text/plain; charset=utf-8");
			StreamUtils.copy(outputFileStream, response.getOutputStream());
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

	@PostMapping(value = "/{buildId}/cancel")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation(value = "Cancel a running build job")
	public void requestCancelBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								   @PathVariable final String buildId, final HttpServletResponse response) throws ResourceNotFoundException, BadConfigurationException {
		buildService.requestCancelBuild(releaseCenterKey, productKey, buildId);
		response.setStatus(HttpStatus.OK.value());
	}

	@GetMapping(value = "/{buildId}/buildLogs")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation(value = "Get the full logs of the build process")
	public void getFullBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								   @PathVariable final String buildId, HttpServletResponse response) throws IOException {
		String logUrl = "/logViewer.html?center="+releaseCenterKey+"&product="+productKey+"&build="+buildId;
		response.sendRedirect(logUrl);
	}

}

