package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.controller.helper.Utils;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.InputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/packages/{packageBusinessKey}")
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] INPUT_FILE_LINKS = {"file"};
	
	public static final String MANIFEST_KEY = "isManifest";

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);
	
	@RequestMapping("/inputfiles")
	@ResponseBody
	public List<Map<String, Object>> getInputFiles(@PathVariable String buildCompositeKey,
												   @PathVariable String packageBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<InputFile> inputFiles = inputFileService.findAll(buildCompositeKey, packageBusinessKey, authenticatedId);
		//TODO Rewrite this with lambda expression once we're on Java8
		List<InputFile> filteredFiles = new Vector<InputFile>();
		for (InputFile file : inputFiles) {
			if (!file.getMetaData().containsKey(MANIFEST_KEY))
				filteredFiles.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(filteredFiles, request, INPUT_FILE_LINKS);
	}

	@RequestMapping("/inputfiles/{inputFileBusinessKey}")
	@ResponseBody
	public Map getInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							@PathVariable String inputFileBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		InputFile inputFile = inputFileService.find(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(inputFile, request, INPUT_FILE_LINKS);
	}

	@RequestMapping(value = "/inputfiles/{inputFileName}", method = RequestMethod.POST)
	@ResponseBody
	public Map uploadInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							   @PathVariable String inputFileName, @RequestParam Part file,
							   HttpServletRequest request) throws IOException {
		return uploadFile(buildCompositeKey, packageBusinessKey, inputFileName, file, request, false);
	}

	@RequestMapping(value = "/inputfiles/{inputFileBusinessKey}/file", produces="application/zip")
	public void getInputFileContents(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
									 @PathVariable String inputFileBusinessKey, HttpServletResponse response) throws IOException {
		getFileContents(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, response);
	}

	@RequestMapping(value = "/inputfiles/{inputFileName}", method = RequestMethod.DELETE)
	public void deleteInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
								@PathVariable String inputFileName, HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		InputFile inputFile = inputFileService.find(buildCompositeKey, packageBusinessKey, inputFileName, authenticatedId);
		inputFileService.delete(inputFile);
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@RequestMapping(value = "/manifest", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getManifest( @PathVariable String buildCompositeKey,
											@PathVariable String packageBusinessKey,
											HttpServletRequest request) {
		InputFile manifest = getManifest(buildCompositeKey, packageBusinessKey);
		return hypermediaGenerator.getEntityHypermedia(manifest, request, INPUT_FILE_LINKS);
	}

	@RequestMapping(value = "/manifest", method = RequestMethod.POST)
	@ResponseBody
	public Map uploadManifestFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
								@RequestParam(value = "file") MultipartFile file,
								HttpServletRequest request) throws IOException {
		//String filename = Utils.getFilename(file, "manifest.xml");
		String filename = file.getOriginalFilename();
		return uploadFile (buildCompositeKey, packageBusinessKey, filename, file, request, true);
	}

	@RequestMapping(value = "/manifest/file", produces="application/zip")
	public void getManifestContents(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
									HttpServletResponse response) throws IOException {
		InputFile manifest = getManifest(buildCompositeKey, packageBusinessKey);
		getFileContents(buildCompositeKey, packageBusinessKey, manifest.getBusinessKey(), response);
	}

	private Map uploadFile(String buildCompositeKey, String packageBusinessKey,
						   String inputFileName, Part file,
						   HttpServletRequest request, boolean isManifest) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();

		long size = file.getSize();
		LOGGER.info("uploadInputFile. inputFileName: {}, size: {}", inputFileName, size);

		InputFile inputFile = inputFileService.createUpdate(buildCompositeKey, packageBusinessKey, inputFileName, file.getInputStream(),
				size, isManifest, authenticatedId);

		return hypermediaGenerator.getEntityHypermediaJustCreated(inputFile, request, INPUT_FILE_LINKS);
	}
	
	private Map uploadFile(String buildCompositeKey, String packageBusinessKey,
			   String inputFileName, MultipartFile file,
			   HttpServletRequest request, boolean isManifest) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		
		long size = file.getSize();
		LOGGER.info("uploadInputFile(multipart). inputFileName: {}, size: {}", inputFileName, size);
		
		InputFile inputFile = inputFileService.createUpdate(buildCompositeKey, packageBusinessKey, inputFileName, file.getInputStream(),
			size, isManifest, authenticatedId);
		
		return hypermediaGenerator.getEntityHypermediaJustCreated(inputFile, request, INPUT_FILE_LINKS);
	}

	private InputFile getManifest(String buildCompositeKey, String packageBusinessKey) {
		String authenticatedId = SecurityHelper.getSubject();
		List<InputFile> inputFiles = inputFileService.findAll(buildCompositeKey, packageBusinessKey, authenticatedId);
		InputFile manifest = null;
		for (InputFile file : inputFiles) {
			if (file.getMetaData().containsKey(MANIFEST_KEY))
				manifest = file;
		}
		return manifest;
	}

	private void getFileContents(	String buildCompositeKey,
									String packageBusinessKey,
									String inputFileBusinessKey,
									HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		InputFile inputFile = inputFileService.find(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, authenticatedId);
		if (inputFile != null) {
			InputStream fileStream = inputFileService.getFileStream(inputFile);
			if (fileStream != null) {
				ServletOutputStream outputStream = response.getOutputStream();
				FileCopyUtils.copy(fileStream, outputStream);
				outputStream.flush();
			}
		} else {
			// File not found
			response.sendError(404);
		}
	}

}
