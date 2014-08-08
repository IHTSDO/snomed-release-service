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
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.InputFileService;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/packages/{packageBusinessKey}")
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String FILENAME = "filename";
	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);


	@RequestMapping(value = "/manifest", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> uploadManifestFile(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
											 @RequestParam(value = "file") final MultipartFile file,
											 final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		inputFileService.putManifestFile(buildCompositeKey, packageBusinessKey, file.getInputStream(), file.getOriginalFilename(), file.getSize(), SecurityHelper.getSubject());
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@RequestMapping(value = "/manifest")
	@ResponseBody
	public Map<String, Object> getManifest(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
										   final HttpServletRequest request) throws ResourceNotFoundException {
		String manifestFileName = inputFileService.getManifestFileName(buildCompositeKey, packageBusinessKey, SecurityHelper.getSubject());
		Map<String, String> objectHashMap = new HashMap<>();
		if (manifestFileName != null) {
			objectHashMap.put(FILENAME, manifestFileName);
			return hypermediaGenerator.getEntityHypermedia(objectHashMap, true, request, "file");
		} else {
			return hypermediaGenerator.getEntityHypermedia(new HashMap<>(), true, request);
		}
	}

	@RequestMapping(value = "/manifest/file", produces="application/xml")
	public void getManifestFile(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
								final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = inputFileService.getManifestStream(buildCompositeKey, packageBusinessKey, SecurityHelper.getSubject())) {
			if (fileStream != null) {
				response.setContentType("application/xml");
				StreamUtils.copy(fileStream, response.getOutputStream());
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to stream manifest file from storage.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/inputfiles", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
											 @RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {

		inputFileService.putInputFile(buildCompositeKey, packageBusinessKey, file.getInputStream(), file.getOriginalFilename(), file.getSize(), SecurityHelper.getSubject());
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@RequestMapping(value = "/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> listInputFiles(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
													final HttpServletRequest request) throws ResourceNotFoundException {
		List<String> filePaths = inputFileService.listInputFilePaths(buildCompositeKey, packageBusinessKey, SecurityHelper.getSubject());
		List<Map<String, String>> files = new ArrayList<>();
		for (String filePath : filePaths) {
			HashMap<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, filePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

	@RequestMapping(value = "/inputfiles/{inputFileName:.*}")
	public void getInputFileFile(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
								@PathVariable final String inputFileName,
								final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = inputFileService.getFileInputStream(buildCompositeKey, packageBusinessKey, inputFileName, SecurityHelper.getSubject())) {
			if (fileStream != null) {
				StreamUtils.copy(fileStream, response.getOutputStream());
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to stream input file from storage.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	//Using Regex to match variable name here due to problems with .txt getting truncated
	//See http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
	@RequestMapping(value = "/inputfiles/{inputFileNamePattern:.+}", method = RequestMethod.DELETE)
	public void deleteInputFile(@PathVariable final String buildCompositeKey, @PathVariable final String packageBusinessKey,
								@PathVariable final String inputFileNamePattern, final HttpServletResponse response) throws IOException, ResourceNotFoundException {
		User authenticatedUser = SecurityHelper.getSubject();
		
		//Are we working with a file name or a pattern?
		if (inputFileNamePattern.contains("*")) {
			inputFileService.deleteFilesByPattern(buildCompositeKey, packageBusinessKey, inputFileNamePattern, authenticatedUser);						
		} else {
		    	try {
		    	    inputFileService.deleteFile(buildCompositeKey, packageBusinessKey, inputFileNamePattern, authenticatedUser);	
		    	} catch (ResourceNotFoundException e) {
		    	    LOGGER.error("Can't find file with name:{} to delete", inputFileNamePattern);
		    	    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		    	    return;
		    	}
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
	
}
