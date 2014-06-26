package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ExecutionService;
import org.ihtsdo.buildcloud.service.PackageService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/executions")
public class ExecutionController {

	@Autowired
	private ExecutionService executionService;
	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private PublishService publishService;

	@Autowired
	private PackageService packageService;

	static final String[] EXECUTION_LINKS = {"configuration", "packages", "buildScripts|build-scripts.zip"};
	static final String[] PACKAGE_LINKS = {"outputfiles"};

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

	@RequestMapping(value = "/{executionId}/packages")
	@ResponseBody
	public List<Map<String, Object>> getPackages(@PathVariable String buildCompositeKey, @PathVariable String executionId,
												 HttpServletRequest request) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		List<ExecutionPackageDTO> executionPackages = executionService.getExecutionPackages(buildCompositeKey, executionId, authenticatedUser);
		return hypermediaGenerator.getEntityCollectionHypermedia(executionPackages, request, PACKAGE_LINKS);
	}

	@RequestMapping(value = "/{executionId}/packages/{packageId}")
	@ResponseBody
	public Map<String, Object> getPackage(@PathVariable String buildCompositeKey, @PathVariable String executionId,
										  @PathVariable String packageId,
										  HttpServletRequest request) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		ExecutionPackageDTO executionPackage = executionService.getExecutionPackage(buildCompositeKey, executionId, packageId, authenticatedUser);
		return hypermediaGenerator.getEntityHypermedia(executionPackage, true, request, PACKAGE_LINKS);
	}

	@RequestMapping(value = "/{executionId}/packages/{packageId}/outputfiles")
	@ResponseBody
	public List<Map<String, Object>> listPackageOutputFiles(@PathVariable String buildCompositeKey, @PathVariable String executionId,
															@PathVariable String packageId,
															HttpServletRequest request) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		List<String> relativeFilePaths = executionService.getExecutionPackageOutputFilePaths(buildCompositeKey, executionId, packageId, authenticatedUser);
		List<Map<String, String>> outputFiles = new ArrayList<>();
		for (String relativeFilePath : relativeFilePaths) {
			HashMap<String, String> file = new HashMap<>();
			file.put("id", relativeFilePath);
			outputFiles.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(outputFiles, request);
	}

	@RequestMapping(value = "/{executionId}/packages/{packageId}/outputfiles/{outputFileName:.*}")
	public void getPackageOutputFile(@PathVariable String buildCompositeKey, @PathVariable String executionId,
											@PathVariable String packageId, @PathVariable String outputFileName,
											HttpServletResponse response) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		try (InputStream outputFileStream = executionService.getOutputFile(buildCompositeKey, executionId, packageId, outputFileName, authenticatedUser)) {
			StreamUtils.copy(outputFileStream, response.getOutputStream());
		}
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

	@RequestMapping(value = "/{executionId}/status/{status}", method = RequestMethod.POST)
	@ResponseBody
	public void setStatus(@PathVariable String buildCompositeKey, @PathVariable String executionId, @PathVariable String status) {
		User authenticatedUser = SecurityHelper.getSubject();
		executionService.updateStatus(buildCompositeKey, executionId, status, authenticatedUser);
	}

	@RequestMapping(value = "/{executionId}/output/publish")
	@ResponseBody
	public void publishReleasePackage( @PathVariable String buildCompositeKey, @PathVariable String executionId){
		
		User authenticatedUser = SecurityHelper.getSubject();
		Execution execution = executionService.find(buildCompositeKey, executionId, authenticatedUser);
		List<Package> packages = packageService.findAll(buildCompositeKey, authenticatedUser);
		for(Package pk: packages ){
			publishService.publishExecutionPackage(execution, pk );
		}
	}

}
