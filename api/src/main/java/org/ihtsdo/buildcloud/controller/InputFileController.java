package org.ihtsdo.buildcloud.controller;

import com.wordnik.swagger.annotations.ApiParam;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.service.ProductInputFileService;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}")
@Api(value = "Input Files", position = 4)
public class InputFileController {

	@Autowired
	private ProductInputFileService productInputFileService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String FILENAME = "filename";

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@RequestMapping(value = "/manifest", method = RequestMethod.POST)
	@ApiOperation( value = "Stores a manifest file",
		notes = "Stores or replaces a file identified as the manifest for the package specified in the URL" )
	@ResponseBody
	public ResponseEntity<Object> uploadManifestFile(@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey, @RequestParam(value = "file") final MultipartFile file)
			throws IOException, ResourceNotFoundException {

		productInputFileService.putManifestFile(releaseCenterKey, productKey, file.getInputStream(), file.getOriginalFilename(), file.getSize());
		InputStream manifestInputStream = productInputFileService.getManifestStream(releaseCenterKey, productKey);
		String validationStatus = ManifestValidator.validate(manifestInputStream);
		if (validationStatus == null) {
			return new ResponseEntity<>(HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(validationStatus, HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}

	@RequestMapping(value = "/manifest", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a manifest file name",
		notes = "Returns a manifest file name for given release center and product" )
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

	@RequestMapping(value = "/manifest/file", produces = "application/xml", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a specified manifest file",
		notes = "Returns the content of the manifest file as xml" )
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
	@ApiOperation( value = "Store or Replace a file",
		notes = "Stores or replaces a file with its original name against the package specified in the URL" )
	@ResponseBody
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {
		String inputFileName = file.getOriginalFilename();
		LOGGER.debug("uploading input file:" + inputFileName);
		if (!Normalizer.isNormalized(inputFileName, Form.NFC)) {
			inputFileName = Normalizer.normalize(inputFileName, Form.NFC);
		}
		productInputFileService.putInputFile(releaseCenterKey, productKey, file.getInputStream(),inputFileName,file.getSize());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@RequestMapping(value = "/inputfiles", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a list of file names",
		notes = "Returns a list of file names for the package specified in the URL" )
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

	@RequestMapping(value = "/inputfiles/{inputFileName:.*}", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a specified file",
		notes = "Returns the content of the specified file." )
	public void getInputFileFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String inputFileName,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = productInputFileService.getFileInputStream(releaseCenterKey, productKey, inputFileName)) {
			response.setContentType("text/plain; charset=utf-8");
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
	@ApiOperation( value = "Returns a specified file",
		notes = "Deletes the specified file, if found. "
			+ "Returns HTTP 404 if the file is not found for the package specified in the URL" )
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

	@RequestMapping(value = "/sourcefiles/{source}", method = RequestMethod.POST)
	@ApiOperation( value = "Store or Replace a file in specified source",
			notes = "Stores or replaces a file in a specified source with its original name against the package specified in the URL. Possible source values are: terminology-server, reference-set-tool, mapping-tools, manual")
	@ResponseBody
	public ResponseEntity<Void> uploadSourceFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable String source,
													@RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {
		if (file != null) {
			String inputFileName = file.getOriginalFilename();
			LOGGER.debug("uploading source file:" + inputFileName);
			if (!Normalizer.isNormalized(inputFileName, Form.NFC)) {
				inputFileName = Normalizer.normalize(inputFileName, Form.NFC);
			}
			try (InputStream inputStream = file.getInputStream()) {
				productInputFileService.putSourceFile(source, releaseCenterKey, productKey, inputStream, inputFileName,file.getSize());
			}
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		throw new IllegalArgumentException("No input source file specified.");
	}

	@RequestMapping(value = "/sourcefiles", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a list of file names in source directories",
			notes = "Returns a list of file names for the package specified in the URL" )
	@ResponseBody
	public List<Map<String, Object>> listSourceFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
													final HttpServletRequest request) throws ResourceNotFoundException {
		List<String> filePaths = productInputFileService.listSourceFilePaths(releaseCenterKey, productKey);
		List<Map<String, String>> files = new ArrayList<>();
		for (String filePath : filePaths) {
			Map<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, filePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

	@RequestMapping(value = "/sourcefiles/{source}/{sourceFileName:.*}", method = RequestMethod.GET)
	@ApiOperation( value = "Returns a specified file",
		notes = "Returns the content of the specified file." )
	public void getSourceFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, 
			@PathVariable String source, @PathVariable final String sourceFileName,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = productInputFileService.getSourceFileStream(releaseCenterKey, productKey, source, sourceFileName)) {
			if (fileStream != null) {
				StreamUtils.copy(fileStream, response.getOutputStream());
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to stream source file from storage.", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	

	@RequestMapping(value = "/sourcefiles/{source}", method = RequestMethod.DELETE)
	@ApiOperation( value = "Returns a specified file",
			notes = "Deletes the specified file, if found. "
					+ "Returns HTTP 404 if the file is not found for the package specified in the URL" )
	public ResponseEntity<Object> deleteSourceFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
												  @PathVariable final String source) throws ResourceNotFoundException {
		productInputFileService.deleteSourceFile(releaseCenterKey, productKey, null, source);
		LOGGER.info("Deleted source files {} for product {}", source, productKey);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/sourcefiles", method = RequestMethod.DELETE)
	@ApiOperation( value = "Returns a specified file",
			notes = "Deletes the files with specified pattern in specified sources, if found. "
					+ "Returns HTTP 404 if the file is not found for the package specified in the URL" )
	public ResponseEntity<Object> deleteSourceFileByPattern(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
															@RequestParam(required = false) final Set<String> sources, @RequestParam final String pattern) throws ResourceNotFoundException {
		productInputFileService.deleteSourceFilesByPattern(releaseCenterKey, productKey, pattern, sources);
		LOGGER.info("Deleted source files by pattern {} for product {}", pattern, productKey);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/inputfiles/prepare", method = RequestMethod.POST)
	@ApiOperation( value = "Prepare input file by processing files in source directories based on configurations in Manifest",
			notes = "Create or replace files in input file directories")
	public ResponseEntity<Object> prepareInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
												   @ApiParam(name = "copyFilesInManifest", value = "Whether to copy unprocessed files specified in manifest into input-files. Default is true")
												   @RequestParam(required = false) final Boolean copyFilesInManifest)throws BusinessServiceException {
		// try avoid to throw exceptions so that build status
		SourceFileProcessingReport report = productInputFileService.prepareInputFiles(releaseCenterKey, productKey, copyFilesInManifest != null ? copyFilesInManifest : true);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@RequestMapping(value = "/inputfiles/prepareReport", produces = "application/json", method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation( value = "Retrieves latest report of input files preparation process",
			notes = "Retrieves input preparation report details for given product key, release center key" )
	public void getBuildReport(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
							   final HttpServletRequest request, final HttpServletResponse response) throws IOException, ResourceNotFoundException {

		try (InputStream outputFileStream = productInputFileService.getInputPrepareReport(releaseCenterKey, productKey)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No report file found");
			}
		}
	}

	@RequestMapping(value = "/inputfiles/gather", method = RequestMethod.POST)
	@ApiOperation(value = "Gather input files from multiple sources and upload to source directories")
	public ResponseEntity<Object> gatherInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								 @RequestBody GatherInputRequestPojo request) throws BusinessServiceException, IOException {
		productInputFileService.gatherSourceFiles(releaseCenterKey, productKey, request, SecurityContextHolder.getContext());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/inputfiles/gatherReport", method = RequestMethod.GET)
	@ApiOperation(value = "Return report of input files gather")
	public void  getInputGatherReport(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, final HttpServletResponse response) throws IOException {
		try (InputStream outputFileStream = productInputFileService.getInputGatherReport(releaseCenterKey, productKey)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No report file found");
			}
		}
	}

	@RequestMapping(value = "/buildLogs", method = RequestMethod.GET)
	@ApiOperation(value = "Get the full logs of the build process")
	public void getFullBuildLogs(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
								 HttpServletResponse response) throws IOException {
		String logUrl = "/logViewer.html?center="+releaseCenterKey+"&product="+productKey;
		response.sendRedirect(logUrl);
	}

	@RequestMapping(value = "/buildLogs/details", method = RequestMethod.GET)
	@ApiOperation(value = "Return report of input files gather")
	public void getFullBuildLogFromProduct(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
										   HttpServletResponse response) {
		try (InputStream outputFileStream = productInputFileService.getFullBuildLogFromProductIfExists(releaseCenterKey, productKey)) {
			if (outputFileStream != null) {
				StreamUtils.copy(outputFileStream, response.getOutputStream());
			} else {
				throw new ResourceNotFoundException("No build log found in product directory. Please find the build log from specific product");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
