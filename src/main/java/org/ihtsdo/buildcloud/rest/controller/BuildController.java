package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.rcarz.jiraclient.JiraException;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.core.entity.RVFFailureJiraAssociation;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.core.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.core.service.RVFFailureJiraAssociationService;
import org.ihtsdo.buildcloud.core.service.helper.ProcessingStatus;
import org.ihtsdo.buildcloud.core.service.manager.ReleaseBuildManager;
import org.ihtsdo.buildcloud.core.service.monitor.MonitorService;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.rest.pojo.BuildPage;
import org.ihtsdo.buildcloud.rest.pojo.BuildRequestPojo;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManager;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsGlobalAdminOrGlobalReleaseManager;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}")
@Tag(name = "Build", description = "-")
public class BuildController {

	private static final String COMMA = ",";

	private final Logger logger = LoggerFactory.getLogger(BuildController.class);

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private PublishService publishService;

	@Autowired
	private ReleaseBuildManager releaseBuildManager;

	@Autowired
	private RVFFailureJiraAssociationService rvfFailureJiraAssociationService;

	@Autowired
	private MonitorService monitorService;

	private static final String[] BUILD_LINKS = {"manifest", "configuration","qaTestConfig", "inputfiles","inputGatherReport", "inputPrepareReport", "outputfiles", "buildReport", "logs", "buildLogs", "preConditionCheckReports", "postConditionCheckReports", "classificationResultsOutputFiles"};

	@Operation(summary="Re-initialise")
	@RequestMapping(value="/builds/initialise", method= RequestMethod.GET)
	public ResponseEntity<Void> initialise() throws BusinessServiceException {
		releaseBuildManager.initialise();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/release", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Create a release package",
			description = "Create a new build and add the build to the job queue automatically")
	public ResponseEntity<Build> createReleasePackage(
			@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey,
			@RequestBody final BuildRequestPojo buildRequestPojo,
			final HttpServletRequest request) throws BusinessServiceException, IOException {
		final String username = SecurityUtil.getUsername();
		final String authenticationToken = SecurityUtil.getAuthenticationToken();
		final Build newBuild = releaseBuildManager.createBuild(releaseCenterKey, productKey, buildRequestPojo, username);
		releaseBuildManager.queueBuild(new CreateReleasePackageBuildRequest(newBuild, username, authenticationToken));
		monitorService.startMonitorBuild(newBuild, username);
		return new ResponseEntity<>(newBuild, HttpStatus.CREATED);
	}

	@PostMapping(value = "/builds")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Create a build",
			description = "Create a build for the given product key and release center key and returns build id")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createBuild(@PathVariable final String releaseCenterKey,
														   @PathVariable final String productKey,
														   @RequestBody final BuildRequestPojo buildRequestPojo,
															final HttpServletRequest request) throws BusinessServiceException {
		final String currentUser = SecurityUtil.getUsername();
		final Build build = releaseBuildManager.createBuild(releaseCenterKey, productKey, buildRequestPojo, currentUser);
		monitorService.startMonitorBuild(build, currentUser);
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(build, false, request, BUILD_LINKS), HttpStatus.CREATED);
	}

	@PostMapping(value = "/builds/{buildId}/clone")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Clone a new release build from specific build",
			description = "Clone from specific build and add the new one to the job queue")
	public ResponseEntity<Build> cloneBuild(
			@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey,
			@PathVariable final String buildId,
			final HttpServletRequest request) throws BusinessServiceException, IOException {
		final String username = SecurityUtil.getUsername();
		final String authenticationToken = SecurityUtil.getAuthenticationToken();

		Build newBuild = buildService.cloneBuild(releaseCenterKey, productKey, buildId, username);
		releaseBuildManager.queueBuild(new CreateReleasePackageBuildRequest(newBuild, username, authenticationToken));
		monitorService.startMonitorBuild(newBuild, username);
		return new ResponseEntity<>(newBuild, HttpStatus.OK);
	}

	@PostMapping(value = "/builds/{buildId}/schedule")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Schedule a release build",
			description = "Add a release build to the job queue")
	public ResponseEntity<Build> scheduleBuild(
			@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey,
			@PathVariable final String buildId,
			final HttpServletRequest request) throws BusinessServiceException, IOException {

		// Verify if the build exists
		Build newBuild  = buildService.find(releaseCenterKey, productKey, buildId, true, true, null , null);
		final String username = SecurityUtil.getUsername();
		final String authenticationToken = SecurityUtil.getAuthenticationToken();
		releaseBuildManager.queueBuild(new CreateReleasePackageBuildRequest(newBuild, username, authenticationToken));
		return new ResponseEntity<>(newBuild, HttpStatus.OK);
	}

	@DeleteMapping(value = "/builds/{buildId}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Delete a build",
			description = "Delete a build for given product key and release center key and build id")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> deleteBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
														   @PathVariable final String buildId, final HttpServletRequest request) throws BusinessServiceException, IOException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);

		ifBuildIsNullThrow(productKey, buildId, build);

		if (!CollectionUtils.isEmpty(build.getTags()) && build.getTags().contains(Build.Tag.PUBLISHED)) {
			throw new BusinessServiceException("You can not delete the PUBLISHED build");
		}

		Build.Status[] BUILD_RUNNING_STATES = {
				Build.Status.PENDING,
				Build.Status.QUEUED,
				Build.Status.BEFORE_TRIGGER,
				Build.Status.BUILDING};
		if (Arrays.asList(BUILD_RUNNING_STATES).contains(build.getStatus())) {
			buildService.markBuildAsDeleted(build);
		} else {
			buildService.delete(releaseCenterKey, productKey, buildId);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/builds")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a list all builds for a logged in user",
			description = "Returns a list all builds visible to the currently logged in user, so this could potentially span across Release Centres")
	@ResponseBody
	public Page<Map<String, Object>> getBuilds(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											   @RequestParam(required = false) boolean includeBuildConfiguration,
											   @RequestParam(required = false) boolean includeQAConfiguration,
											   @RequestParam(required = false) Boolean visibility,
											   @RequestParam(defaultValue = "DEFAULT") BuildService.View viewMode,
											   @RequestParam(required = false) List<Integer> forYears,
											   @RequestParam(defaultValue = "0") Integer pageNumber,
											   @RequestParam(defaultValue = "10") Integer pageSize,
											   @RequestParam(required = false) String[] sort,
											   final HttpServletRequest request) throws ResourceNotFoundException {
		PageRequest pageRequest;
		if (sort != null && sort.length != 0) {
			List<Sort.Order> orders = createPageRequest(sort);
			pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(orders));
		} else {
			pageRequest = PageRequest.of(pageNumber, pageSize);
		}
		BuildPage<Build> builds = buildService.findAll(releaseCenterKey, productKey, includeBuildConfiguration, includeQAConfiguration, true, visibility, viewMode, forYears, pageRequest);
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(builds.getContent(), request, BUILD_LINKS);

		return new PageImpl<>(result, PageRequest.of(pageNumber, pageSize), builds.getTotalElements());
	}

	@GetMapping(value = "/builds/published")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Returns a list of published builds",
			description = "Returns a list of published builds from the published releases directory")
	@ResponseBody
	public List<Map<String, Object>> getPublishedBuilds(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
														@RequestParam(required = false, defaultValue = "false") boolean includeProdPublishedReleases,
											   final HttpServletRequest request) throws ResourceNotFoundException {
		List<Build> builds = publishService.findPublishedBuilds(releaseCenterKey, productKey, includeProdPublishedReleases);
		return hypermediaGenerator.getEntityCollectionHypermediaOfAction(builds, request, BUILD_LINKS, null);
	}

	@GetMapping(value = "/builds/{buildId}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Get a build id",
			description = "Returns a single build object for given key")
	public Map<String, Object> getBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										@RequestParam(required = false, defaultValue = "true") boolean includeBuildConfiguration,
										@RequestParam(required = false, defaultValue = "true") boolean includeQAConfiguration,
										@RequestParam(required = false) Boolean visibility,
										@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, includeBuildConfiguration, includeQAConfiguration, true, visibility);

		ifBuildIsNullThrow(productKey, buildId, build);

		return hypermediaGenerator.getEntityHypermedia(build, true, request, BUILD_LINKS);
	}

	@GetMapping(value = "/builds/{buildId}/manifest", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Returns a manifest file name",
			description = "Returns a manifest file name for given product key, release center key, and build id")
	public Map<String, Object> getManifest(@PathVariable final String releaseCenterKey,
										   @PathVariable final String productKey,
										   @PathVariable final String buildId,
										   final HttpServletRequest request) throws ResourceNotFoundException {
		String manifestFileName = buildService.getManifestFileName(releaseCenterKey, productKey, buildId);
		Map<String, String> objectHashMap = new HashMap<>();
		if (manifestFileName != null) {
			objectHashMap.put("filename", manifestFileName);
			return hypermediaGenerator.getEntityHypermedia(objectHashMap, true, request, "file");
		} else {
			return hypermediaGenerator.getEntityHypermedia(new HashMap<>(), true, request);
		}
	}

	@GetMapping(value = "/builds/{buildId}/manifest/file", produces = "application/xml")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a specified manifest file",
			description = "Returns the content of the manifest file as xml")
	public void getManifestFile(@PathVariable final String releaseCenterKey,
								@PathVariable final String productKey,
								@PathVariable final String buildId,
								final HttpServletResponse response) throws ResourceNotFoundException {
		try (InputStream fileStream = buildService.getManifestStream(releaseCenterKey, productKey, buildId)) {
			if (fileStream != null) {
				response.setContentType("application/xml");
				StreamUtils.copy(fileStream, response.getOutputStream());
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException e) {
			logger.error("Failed to stream manifest file from storage.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(value = "/builds/{buildId}/configuration", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves configuration details",
			description = "Retrieves configuration details for given product key, release center key, and build id")
	public Map<String, Object> getConfiguration(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			final HttpServletRequest request) throws IOException, BusinessServiceException {
		final BuildConfiguration buildConfiguration = buildService.loadBuildConfiguration(releaseCenterKey, productKey, buildId);
		final Map<String,Object> result = new HashMap<>();
		if (buildConfiguration != null ) {
			result.putAll(hypermediaGenerator.getEntityHypermedia(buildConfiguration, true, request));
		}
		return result;
	}
	
	
	@GetMapping(value = "/builds/{buildId}/qaTestConfig", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves QA test configuration details",
			description = "Retrieves configuration details for given product key, release center key, and build id")
	public Map<String, Object> getQqTestConfig(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			final HttpServletRequest request) throws IOException, BusinessServiceException {
		final Map<String,Object> result = new HashMap<>();
		final QATestConfig qaTestConfig = buildService.loadQATestConfig(releaseCenterKey, productKey, buildId);
		if( qaTestConfig != null) {
			result.putAll(hypermediaGenerator.getEntityHypermedia(qaTestConfig, true, request));
		}
		return result;
	}
	
	
	@GetMapping(value = "/builds/{buildId}/buildReport", produces = "application/json")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves build report details",
			description = "Retrieves buildReport details for given product key, release center key, and build id")
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

	@GetMapping(value = "/builds/{buildId}/inputfiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves list of input file names",
			description = "Retrieves list of input file names for given release center, product key and build id")
	public List<Map<String, Object>> listPackageInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {

		final List<String> relativeFilePaths = buildService.getInputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}
	
	@GetMapping(value = "/builds/{buildId}/inputPrepareReport")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves the report for preparing input source files",
			description = "Product key and build id are required. And the report might not exist if no preparation is required")
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

	@GetMapping(value = "/builds/{buildId}/inputGatherReport")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves the report for gathering input source files",
			description = "Product key and build id are required. And the report might not exist if no preparation is required")
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


	@GetMapping(value = "/builds/{buildId}/inputfiles/{inputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Download a specific file",
			description = "Download a specific file content for given release center, product key, build id and given input file name combination")
	public void getPackageInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String inputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getInputFile(releaseCenterKey, productKey, buildId, inputFileName)) {
			if (outputFileStream == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			} else {
				response.setContentType("text/plain; charset=utf-8");
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			}
		}
	}

	@GetMapping(value = "/builds/{buildId}/outputfiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Retrieves a list of file names from output directory",
			description = "Retrieves a list of file names from output directory for given release center, product key, build id combination")
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws BusinessServiceException {

		final List<String> relativeFilePaths = buildService.getOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@GetMapping(value = "/builds/{buildId}/outputfiles/{outputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Download a specific file from output directory",
			description = "Download a specific file from output directory for given release center, product key, build id and file name combination")
	public void getPackageOutputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@PathVariable final String outputFileName, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = buildService.getOutputFile(releaseCenterKey, productKey, buildId, outputFileName)) {
			response.setContentType("text/plain; charset=utf-8");
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
	}

	@PostMapping(value = "/builds/{buildId}/visibility")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Update visibility for build",
			description = "Update an existing build with the visibility flag")
	public ResponseEntity<Void> updateVisibility(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										   @PathVariable final String buildId, @RequestParam(required = true, defaultValue = "true") boolean visibility) throws IOException {
		buildService.updateVisibility(releaseCenterKey, productKey, buildId, visibility);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/builds/{buildId}/tags")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Update tags for build",
			description = "Update an existing build with the tags")
	public ResponseEntity<Void> updateTags(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										   @PathVariable final String buildId, @RequestParam(required = true) List<Build.Tag> tags) throws IOException {
		final Build build = buildService.find(releaseCenterKey, productKey, buildId, null, null, null, null);
		buildService.saveTags(build, tags);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/builds/{buildId}/publish")
	@IsAuthenticatedAsGlobalAdminOrGlobalReleaseManager
	@ResponseBody
	@Operation(summary = "Publish a release for given build id",
			description = "Publish release for given build id to make it available in repository for wider usages")
	public void publishBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, @RequestParam(required = false) String environment) throws BusinessServiceException {

		final Build build = buildService.find(releaseCenterKey, productKey, buildId, true, null, null, null);
		ifBuildIsNullThrow(productKey, buildId, build);
		publishService.publishBuildAsync(build, true, environment);
	}

	@GetMapping(value = "/builds/{buildId}/publish/status")
	@IsAuthenticatedAsAdminOrReleaseManager
	@ResponseBody
	@Operation(summary = "Get publishing release status",
			description = "Get publishing release status for given build id")
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

	@GetMapping(value = "/builds/{buildId}/logs")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Retrieves a list of build log file names",
			description = "Retrieves a list of build log file names for given release center, product key, and build id")
	public List<Map<String, Object>> getBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, final HttpServletRequest request) throws ResourceNotFoundException {

		return convertFileListToEntities(request, buildService.getLogFilePaths(releaseCenterKey, productKey, buildId));
	}

	@GetMapping(value = "/builds/{buildId}/logs/{logFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Download a specific build log file",
			description = "Download a specific log file for given release center, product key, build id and file name combination")
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

	@RequestMapping(value = "/builds/{buildId}/logs/{logFileName:.*}", method = RequestMethod.HEAD)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Download a specific build log file",
			description = "Download a specific log file for given release center, product key, build id and file name combination")
	public void getBuildLogHead(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String buildId, @PathVariable final String logFileName, final HttpServletResponse response)
			throws ResourceNotFoundException, IOException {

		try (InputStream outputFileStream = buildService.getLogFile(releaseCenterKey, productKey, buildId, logFileName)) {
			// This will blow up with 404 if the file isn't found. HTTP HEAD demands that no body is returned, so nothing to do here
			logger.debug("HTTP 200 response to head request for {}/{}/{}", productKey, buildId, logFileName);
		}
	}

	@GetMapping(value = "/builds/{buildId}/preConditionCheckReports")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves Pre-Condition Check Report",
			description = "Retrieves configuration details for given product key, release center key, and build id")
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

	@GetMapping(value = "/builds/{buildId}/postConditionCheckReports")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves Post-Condition Check Report",
			description = "Retrieves configuration details for given product key, release center key, and build id")
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

	@GetMapping(value = "/builds/{buildId}/classificationResultsOutputFiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ResponseBody
	@Operation(summary = "Retrieves classification results for output files",
			description = "Retrieves configuration details for given product key, release center key, and build id")
	public List<Map<String, Object>> getClassificationResultsOutputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											 @PathVariable final String buildId, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {
		final List<String> relativeFilePaths = buildService.getClassificationResultOutputFilePaths(releaseCenterKey, productKey, buildId);
		return convertFileListToEntities(request, relativeFilePaths);
	}

	@GetMapping(value = "/builds/{buildId}/classificationResultsOutputFiles/{inputFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Download a specific file",
			description = "Download a specific file content for given release center, product key, build id and given file name combination")
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

	@PostMapping(value = "/builds/{buildId}/cancel")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Cancel a running build job")
	public void requestCancelBuild(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								   @PathVariable final String buildId, final HttpServletResponse response) throws ResourceNotFoundException, BadConfigurationException, IOException {
		buildService.requestCancelBuild(releaseCenterKey, productKey, buildId);
		response.setStatus(HttpStatus.OK.value());
	}

	@GetMapping(value = "/builds/{buildId}/buildLogs")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Get the full logs of the build process")
	public void getFullBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								   @PathVariable final String buildId, HttpServletResponse response) throws IOException {
		String logUrl = "/logViewer.html?center=" + releaseCenterKey + "&product=" + productKey + "&build=" + buildId;
		response.sendRedirect(logUrl);
	}

	@GetMapping(value = "/builds/{buildId}/failure-jira-associations")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Get the list of JIRA issues associated with the RVF failures for each build")
	public ResponseEntity<List<RVFFailureJiraAssociation>> getRVFFailureJiraAssociations(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
																						 @PathVariable final String buildId) {
		return new ResponseEntity<>(rvfFailureJiraAssociationService.findByBuildKey(releaseCenterKey, productKey, buildId), HttpStatus.OK);
	}

	@PostMapping(value = "/builds/{buildId}/failure-jira-associations")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Generate Jira issues for the RVF failures")
	public ResponseEntity<Map<String, List<RVFFailureJiraAssociation>>>  createRVFFailureJiraAssociations(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
											  @PathVariable final String buildId, @RequestBody String[] assertionIds) throws BusinessServiceException, IOException, JiraException {
		return new ResponseEntity<>(rvfFailureJiraAssociationService.createFailureJiraAssociations(releaseCenterKey, productKey, buildId, assertionIds), HttpStatus.CREATED);
	}

	private List<Sort.Order> createPageRequest(String[] sort) {
		List<Sort.Order> orders = new ArrayList<>();
		if (sort.length == 2 && !sort[0].contains(COMMA)) {
			String fieldSortProperty = sort[0];
			String fieldSortDirection = sort[1];
			Sort.Direction sortDirection = (fieldSortDirection != null && fieldSortDirection.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
			Sort.Order order = new Sort.Order(sortDirection, fieldSortProperty);
			orders.add(order);
		} else {
			for(String item : sort) {
				String[] arr = item.split(COMMA);
				String fieldSortProperty = arr[0];
				String fieldSortDirection = arr[1];
				Sort.Direction sortDirection = (fieldSortDirection != null && fieldSortDirection.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
				Sort.Order order = new Sort.Order(sortDirection, fieldSortProperty);
				orders.add(order);
			}
		}
		return orders;
	}
}

