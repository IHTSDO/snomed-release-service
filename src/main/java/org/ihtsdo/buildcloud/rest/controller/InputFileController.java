package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.core.service.InputFileService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.BuildRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.*;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products/{productKey}")
@Api(value = "Input Files", position = 4)
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private ProductService productService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String FILENAME = "filename";

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@PostMapping(value = "/manifest")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Stores a manifest file",
		notes = "Stores or replaces a file identified as the manifest for the package specified in the URL" )
	@ResponseBody
	public ResponseEntity<Object> uploadManifestFile(@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey, @RequestParam(value = "file") final MultipartFile file)
			throws IOException, ResourceNotFoundException {

		inputFileService.putManifestFile(releaseCenterKey, productKey, file.getInputStream(), file.getOriginalFilename(), file.getSize());
		InputStream manifestInputStream = inputFileService.getManifestStream(releaseCenterKey, productKey);
		String validationStatus = ManifestValidator.validate(manifestInputStream);
		if (validationStatus == null) {
			return new ResponseEntity<>(HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(validationStatus, HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}

	@GetMapping(value = "/manifest")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a manifest file name",
		notes = "Returns a manifest file name for given release center and product" )
	@ResponseBody
	public Map<String, Object> getManifest(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletRequest request) throws ResourceNotFoundException {
		String manifestFileName = inputFileService.getManifestFileName(releaseCenterKey, productKey);
		Map<String, String> objectHashMap = new HashMap<>();
		if (manifestFileName != null) {
			objectHashMap.put(FILENAME, manifestFileName);
			return hypermediaGenerator.getEntityHypermedia(objectHashMap, true, request, "file");
		} else {
			return hypermediaGenerator.getEntityHypermedia(new HashMap<>(), true, request);
		}
	}

	@GetMapping(value = "/manifest/file", produces = "application/xml")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a specified manifest file",
		notes = "Returns the content of the manifest file as xml" )
	public void getManifestFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = inputFileService.getManifestStream(releaseCenterKey, productKey)) {
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

	@PostMapping(value = "/builds/{buildId}/inputfiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Store or Replace a file",
		notes = "Stores or replaces a file with its original name against the package specified in the URL" )
	@ResponseBody
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
			@RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {
		String inputFileName = file.getOriginalFilename();
		LOGGER.debug("uploading input file:" + inputFileName);
		if (!Normalizer.isNormalized(inputFileName, Form.NFC)) {
			inputFileName = Normalizer.normalize(inputFileName, Form.NFC);
		}
		Product product = productService.find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
		if (product.getBuildConfiguration() != null && !product.getBuildConfiguration().isJustPackage()) {
			if (file.getName().startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
				inputFileName = inputFileName.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "");
			}
			inputFileName = inputFileName.replace("der2", "rel2").replace("sct2", "rel2");
		}

		inputFileService.putInputFile(releaseCenterKey, product, buildId, file.getInputStream(),inputFileName,file.getSize());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping(value = "/builds/{buildId}/sourcefiles/{source}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Store or Replace a file in specified source",
			notes = "Stores or replaces a file in a specified source with its original name against the package specified in the URL. Possible source values are: terminology-server, reference-set-tool, mapping-tools, manual")
	@ResponseBody
	public ResponseEntity<Void> uploadSourceFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,@PathVariable String buildId, @PathVariable String source,
													@RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {
		if (file != null) {
			String inputFileName = file.getOriginalFilename();
			LOGGER.debug("uploading source file:" + inputFileName);
			if (!Normalizer.isNormalized(inputFileName, Form.NFC)) {
				inputFileName = Normalizer.normalize(inputFileName, Form.NFC);
			}
			try (InputStream inputStream = file.getInputStream()) {
				inputFileService.putSourceFile(source, releaseCenterKey, productKey, buildId, inputStream, inputFileName,file.getSize());
			}
			return new ResponseEntity<>(HttpStatus.CREATED);
		}
		throw new IllegalArgumentException("No input source file specified.");
	}

	@GetMapping(value = "/builds/{buildId}/sourcefiles")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a list of file names in source directories",
			notes = "Returns a list of file names for the package specified in the URL" )
	@ResponseBody
	public List<Map<String, Object>> listSourceFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
													final HttpServletRequest request) throws ResourceNotFoundException {
		List<String> filePaths = inputFileService.listSourceFilePaths(releaseCenterKey, productKey, buildId);
		List<Map<String, String>> files = new ArrayList<>();
		for (String filePath : filePaths) {
			Map<String, String> file = new HashMap<>();
			file.put(ControllerConstants.ID, filePath);
			files.add(file);
		}
		return hypermediaGenerator.getEntityCollectionHypermedia(files, request);
	}

	@GetMapping(value = "/builds/{buildId}/sourcefiles/{source}/{sourceFileName:.*}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a specified file",
		notes = "Returns the content of the specified file." )
	public void getSourceFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey,
			@PathVariable final String source, @PathVariable final String buildId, @PathVariable final String sourceFileName,
			final HttpServletResponse response) throws ResourceNotFoundException {

		try (InputStream fileStream = inputFileService.getSourceFileStream(releaseCenterKey, productKey, source, sourceFileName)) {
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


	@PostMapping(value = "/builds/{buildId}/inputfiles/prepare")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Prepare input file by processing files in source directories based on configurations in Manifest",
			notes = "Create or replace files in input file directories")
	public ResponseEntity<Object> prepareInputFile(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
												   @ApiParam(name = "copyFilesInManifest", value = "Whether to copy unprocessed files specified in manifest into input-files. Default is true")
												   @RequestParam(required = false) final Boolean copyFilesInManifest) throws BusinessServiceException {
		// try avoid to throw exceptions so that build status
		SourceFileProcessingReport report = inputFileService.prepareInputFiles(releaseCenterKey, productKey, buildId, copyFilesInManifest != null ? copyFilesInManifest : true);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping(value = "/builds/{buildId}/inputfiles/gather")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation(value = "Gather input files from multiple sources and upload to source directories")
	public ResponseEntity<Object> gatherInputFiles(@PathVariable final String releaseCenterKey, @PathVariable final String productKey, @PathVariable final String buildId,
								 @RequestBody BuildRequestPojo request) throws BusinessServiceException, IOException {
		inputFileService.gatherSourceFiles(releaseCenterKey, productKey, buildId, request, SecurityContextHolder.getContext());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
