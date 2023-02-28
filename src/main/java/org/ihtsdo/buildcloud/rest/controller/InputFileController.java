package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.BuildService;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.manifest.ManifestValidator;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.core.service.InputFileService;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
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
@Tag(name = "Input Files", description = "-")
public class InputFileController {

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private ProductService productService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String FILENAME = "filename";

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileController.class);

	@PostMapping(value = "/manifest")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Stores a manifest file",
			description = "Stores or replaces a file identified as the manifest for the package specified in the URL")
	@ResponseBody
	public ResponseEntity<Object> uploadManifestFile(@PathVariable final String releaseCenterKey,
													 @PathVariable final String productKey,
													 @RequestParam(value = "file") final MultipartFile file) throws IOException, ResourceNotFoundException {
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
	@Operation(summary = "Returns a manifest file name",
			description = "Returns a manifest file name for given release center and product")
	@ResponseBody
	public Map<String, Object> getManifest(@PathVariable final String releaseCenterKey,
										   @PathVariable final String productKey,
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
	@Operation(summary = "Returns a specified manifest file",
			description = "Returns the content of the manifest file as xml")
	public void getManifestFile(@PathVariable final String releaseCenterKey,
								@PathVariable final String productKey,
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
	@Operation(summary = "Store or replace a file",
			description = "Stores or replaces a file with its original name against the package specified in the URL")
	@ResponseBody
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable final String releaseCenterKey,
													@PathVariable final String productKey,
													@PathVariable final String buildId,
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

		inputFileService.putInputFile(releaseCenterKey, productKey, buildId, file.getInputStream(),inputFileName,file.getSize());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping(value = "/builds/{buildId}/sourcefiles/{source}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Store or replace a file in specified source",
			description = "Stores or replaces a file in a specified source with its original name against the package specified in the URL. " +
					"Possible source values are: terminology-server, reference-set-tool, mapping-tools, manual")
	@ResponseBody
	public ResponseEntity<Void> uploadSourceFile(@PathVariable final String releaseCenterKey,
												 @PathVariable final String productKey,
												 @PathVariable String buildId,
												 @PathVariable String source,
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
	@Operation(summary = "Returns a list of file names in source directories",
			description = "Returns a list of file names for the package specified in the URL")
	@ResponseBody
	public List<Map<String, Object>> listSourceFiles(@PathVariable final String releaseCenterKey,
													 @PathVariable final String productKey,
													 @PathVariable final String buildId,
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
	@Operation(summary = "Returns a specified file",
			description = "Returns the content of the specified file")
	public void getSourceFile(@PathVariable final String releaseCenterKey,
							  @PathVariable final String productKey,
							  @PathVariable final String buildId,
							  @PathVariable final String source,
							  @PathVariable final String sourceFileName,
							  final HttpServletResponse response) throws ResourceNotFoundException {
		try (InputStream fileStream = inputFileService.getSourceFileStream(releaseCenterKey, productKey, buildId, source, sourceFileName)) {
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
	@Operation(summary = "Prepare input files",
			description = "Prepare input files by processing files in source directories based on configurations in manifest. Creates or replaces files in input file directories")
	public ResponseEntity<Object> prepareInputFile(@PathVariable final String releaseCenterKey,
												   @PathVariable final String productKey,
												   @PathVariable final String buildId,
												   @Parameter(name = "copyFilesInManifest", description = "Whether to copy unprocessed files specified in manifest into input-files. Default is true")
													   @RequestParam(required = false) final Boolean copyFilesInManifest)
			throws BusinessServiceException {
		// try avoid to throw exceptions so that build status
		Build build = buildService.find(releaseCenterKey, productKey, buildId, true, true, false, false);
		if (build == null) {
			throw new ResourceNotFoundException(String.format("Unable to find build id %s for productKey %s", buildId, productKey));
		}
		inputFileService.prepareInputFiles(build, copyFilesInManifest != null ? copyFilesInManifest : true);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping(value = "/builds/{buildId}/inputfiles/gather")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Gather input files",
			description = "Gather input files from multiple sources and upload to source directories")
	public ResponseEntity<Object> gatherInputFiles(@PathVariable final String releaseCenterKey,
												   @PathVariable final String productKey,
												   @PathVariable final String buildId) throws BusinessServiceException, IOException {
		Build build  = buildService.find(releaseCenterKey, productKey, buildId, true, true, false, false);
		if (build == null) {
			throw new ResourceNotFoundException(String.format("Unable to find build id %s for productKey %s", buildId, productKey));
		}
		inputFileService.gatherSourceFiles(build, SecurityContextHolder.getContext());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
