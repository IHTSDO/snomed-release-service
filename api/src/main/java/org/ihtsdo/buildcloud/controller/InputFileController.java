package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.controller.helper.LinkPath;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.InputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/packages/{packageBusinessKey}/inputfiles")
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final LinkPath[] INPUT_FILE_LINKS = { new LinkPath ("file")};

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getInputFiles(@PathVariable String buildCompositeKey,
												   @PathVariable String packageBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<InputFile> inputFiles = inputFileService.findAll(buildCompositeKey, packageBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(inputFiles, request, INPUT_FILE_LINKS);
	}

	@RequestMapping("/{inputFileBusinessKey}")
	@ResponseBody
	public Map getInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							@PathVariable String inputFileBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		InputFile inputFile = inputFileService.find(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(inputFile, request, INPUT_FILE_LINKS);
	}

	@RequestMapping(value = "/{inputFileName}", method = RequestMethod.POST)
	@ResponseBody
	public Map uploadInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							   @PathVariable String inputFileName, @RequestParam Part file,
							   HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();

		long size = file.getSize();
		LOGGER.info("uploadInputFile. inputFileName: {}, size: {}", inputFileName, size);

		InputFile inputFile = inputFileService.createUpdate(buildCompositeKey, packageBusinessKey, inputFileName, file.getInputStream(),
				size, authenticatedId);

		return hypermediaGenerator.getEntityHypermediaJustCreated(inputFile, request, INPUT_FILE_LINKS);
	}

	@RequestMapping(value = "/{inputFileBusinessKey}/file", produces="application/zip")
	public void getInputFileContents(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
													@PathVariable String inputFileBusinessKey, HttpServletResponse response) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();
		InputFile inputFile = inputFileService.find(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, authenticatedId);
		if (inputFile != null) {
			InputStream fileStream = inputFileService.getFileStream(inputFile);
			if (fileStream != null) {
				ServletOutputStream outputStream = response.getOutputStream();
				FileCopyUtils.copy(fileStream, outputStream);
				outputStream.flush();
			}
		}

		// File not found
		response.sendError(404);
	}

}
