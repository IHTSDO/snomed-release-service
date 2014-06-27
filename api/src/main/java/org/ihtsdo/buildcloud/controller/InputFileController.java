package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.InputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public ResponseEntity uploadManifestFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
											 @RequestParam(value = "file") MultipartFile file,
											 HttpServletResponse response) throws IOException {

		inputFileService.putManifestFile(buildCompositeKey, packageBusinessKey, file.getInputStream(), file.getOriginalFilename(), file.getSize(), SecurityHelper.getSubject());
		return new ResponseEntity(HttpStatus.OK);
	}

	@RequestMapping(value = "/manifest")
	@ResponseBody
	public Map<String, Object> getManifest(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
										   HttpServletRequest request) {
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
	public void getManifestFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
								HttpServletResponse response) {

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
	public ResponseEntity uploadInputFileFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
											 @RequestParam(value = "file") MultipartFile file) throws IOException {

		inputFileService.putInputFile(buildCompositeKey, packageBusinessKey, file.getInputStream(), file.getOriginalFilename(), file.getSize(), SecurityHelper.getSubject());
		return new ResponseEntity(HttpStatus.OK);
	}

	@RequestMapping(value = "/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> listInputFiles(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
													HttpServletRequest request) {
		List<String> filePaths = inputFileService.listInputFilePaths(buildCompositeKey, packageBusinessKey, SecurityHelper.getSubject());
		List<Map<String, String>> files = new ArrayList<>();
		for (String filePath : filePaths) {
			HashMap<String, String> file = new HashMap<>();
			file.put("id", filePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

	@RequestMapping(value = "/inputfiles/{inputFileName:.*}")
	public void getInputFileFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
								@PathVariable String inputFileName,
								HttpServletResponse response) {

		try (InputStream fileStream = inputFileService.getFileInputStream(buildCompositeKey, packageBusinessKey, inputFileName, SecurityHelper.getSubject())) {
			if (fileStream != null) {
				StreamUtils.copy(fileStream, response.getOutputStream());
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to stream manifest file from storage.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	//Using Regex to match variable name here due to problems with .txt getting truncated
	//See http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
	@RequestMapping(value = "/inputfiles/{inputFileNamePattern:.+}", method = RequestMethod.DELETE)
	public void deleteInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
								@PathVariable String inputFileNamePattern, HttpServletResponse response) throws IOException {
		User authenticatedUser = SecurityHelper.getSubject();
		
		//Are we working with a file name or a pattern?
		if (inputFileNamePattern.contains("*")) {
			inputFileService.deleteFilesByPattern(buildCompositeKey, packageBusinessKey, inputFileNamePattern, authenticatedUser);						
		} else {
			inputFileService.deleteFile(buildCompositeKey, packageBusinessKey, inputFileNamePattern, authenticatedUser);						
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
	
}
