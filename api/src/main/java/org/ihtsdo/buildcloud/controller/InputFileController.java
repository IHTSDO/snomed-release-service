package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}")
public class InputFileController {

	@Autowired
	private ProductInputFileService productInputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String FILENAME = "filename";

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@RequestMapping(value = "/manifest", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> uploadManifestFile(@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey, @RequestParam(value = "file") final MultipartFile file)
			throws IOException, ResourceNotFoundException {

		productInputFileService.putManifestFile(releaseCenterKey, productKey, file.getInputStream(), file.getOriginalFilename(), file.getSize());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@RequestMapping(value = "/manifest")
	@ResponseBody
	public Map<String, Object> getManifest(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws ResourceNotFoundException {
		String manifestFileName = productInputFileService.getManifestFileName(releaseCenterKey, productKey);
		Map<String, String> objectHashMap = new HashMap<>();
		if (manifestFileName != null) {
			objectHashMap.put(FILENAME, manifestFileName);
			return hypermediaGenerator.getEntityHypermedia(objectHashMap, true, request, "file");
		} else {
			return hypermediaGenerator.getEntityHypermedia(new HashMap<>(), true, request);
		}
	}

	@RequestMapping(value = "/manifest/file", produces = "application/xml")
	public void getManifestFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = productInputFileService.getManifestStream(releaseCenterKey, productKey)) {
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
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {

		productInputFileService.putInputFile(releaseCenterKey, productKey, file.getInputStream(), file.getOriginalFilename(), file.getSize());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@RequestMapping(value = "/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> listInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws ResourceNotFoundException {
		List<String> filePaths = productInputFileService.listInputFilePaths(releaseCenterKey, productKey);
		List<Map<String, String>> files = new ArrayList<>();
		for (String filePath : filePaths) {
			Map<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, filePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

	@RequestMapping(value = "/inputfiles/{inputFileName:.*}")
	public void getInputFileFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String inputFileName,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = productInputFileService.getFileInputStream(releaseCenterKey, productKey, inputFileName)) {
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

	// Using Regex to match variable name here due to problems with .txt getting truncated
	// See http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
	@RequestMapping(value = "/inputfiles/{inputFileNamePattern:.+}", method = RequestMethod.DELETE)
	public ResponseEntity<Object> deleteInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String inputFileNamePattern) throws IOException, ResourceNotFoundException {

		// Are we working with a file name or a pattern?
		if (inputFileNamePattern.contains("*")) {
			productInputFileService.deleteFilesByPattern(releaseCenterKey, productKey, inputFileNamePattern);
		} else {
			productInputFileService.deleteFile(releaseCenterKey, productKey, inputFileNamePattern);
		}
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
