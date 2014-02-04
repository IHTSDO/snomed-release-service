package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.InputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/builds/{buildCompositeKey}/packages/{packageBusinessKey}/input-files")
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] INPUT_FILE_LINKS = {};

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getInputFiles(@PathVariable String buildCompositeKey,
												   @PathVariable String packageBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Set<InputFile> inputFiles = inputFileService.findAll(buildCompositeKey, packageBusinessKey, authenticatedId);
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

	@RequestMapping(value = "/{inputFileBusinessKey}", method = RequestMethod.POST)
	@ResponseBody
	public Map uploadInputFile(@PathVariable String buildCompositeKey, @PathVariable String packageBusinessKey,
							   @PathVariable String inputFileBusinessKey, @RequestParam Part file,
							   HttpServletRequest request) throws IOException {
		String authenticatedId = SecurityHelper.getSubject();

		long size = file.getSize();
		LOGGER.info("uploadInputFile. inputFileBusinessKey: {}, size: {}", inputFileBusinessKey, size);

		InputFile inputFile = inputFileService.create(buildCompositeKey, packageBusinessKey, inputFileBusinessKey, file.getInputStream(),
				size, authenticatedId);

		return hypermediaGenerator.getEntityHypermedia(inputFile, request, INPUT_FILE_LINKS);
	}

}
