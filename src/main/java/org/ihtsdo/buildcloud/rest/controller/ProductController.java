package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.ManifestConfig;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.manifest.generation.ReleaseManifestService;
import org.ihtsdo.buildcloud.core.releaseinformation.ConceptMini;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static org.ihtsdo.buildcloud.core.service.ProductService.PRODUCT_LINKS;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@RestController
@RequestMapping(value = "/centers/{releaseCenterKey}/products", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Product", description = "-")
	public class ProductController {

	public static final String UNABLE_TO_FIND_PRODUCT_ERROR = "Unable to find product: ";
	private final ProductService productService;

	private final HypermediaGenerator hypermediaGenerator;

	private final ReleaseManifestService releaseManifestService;

	@Value("${srs.manifest.optional-refsets}")
	private String optionalManifestRefsetsConfig;


	@Autowired
	public ProductController(ProductService productService, HypermediaGenerator hypermediaGenerator, ReleaseManifestService releaseManifestService) {
		this.productService = productService;
		this.hypermediaGenerator = hypermediaGenerator;
		this.releaseManifestService = releaseManifestService;
	}

	@GetMapping
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a list of products",
			description = "Returns a list of products for the extension specified in the URL")
	public Page<Map<String, Object>> getProducts(@PathVariable String releaseCenterKey,
												 @RequestParam(required = false) boolean includeRemoved,
												 @RequestParam(required = false) boolean includeLegacy,
												 @RequestParam(required = false) boolean includedLatestBuildStatusAndTags,
												 @RequestParam(defaultValue = "0") Integer pageNumber,
												 @RequestParam(defaultValue = "10") Integer pageSize,
												 @RequestParam(required = false) String sortField,
												 @RequestParam(required = false) String sortDirection,
												 HttpServletRequest request) {
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (includeRemoved) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}
		if (includeLegacy) {
			filterOptions.add(FilterOption.INCLUDE_LEGACY);
		}

		PageRequest pageRequest = PageRequestHelper.createPageRequest(pageNumber, pageSize,
				StringUtils.isEmpty(sortField) ? null : Collections.singletonList(sortField),
				StringUtils.isEmpty(sortDirection) ? null : Collections.singletonList(sortDirection));
		Page<Product> page = productService.findAll(releaseCenterKey, filterOptions, pageRequest, includedLatestBuildStatusAndTags);
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(page.getContent(), request, PRODUCT_LINKS.toArray(String[]::new));

		return new PageImpl<>(result, pageRequest, page.getTotalElements());
	}

	@GetMapping( value = "/hidden")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a list of  hidden products",
			description = "Returns a list of hidden products for the extension specified in the URL")
	public Page<Map<String, Object>> getHiddenProducts(@PathVariable String releaseCenterKey,
	                                             @RequestParam(defaultValue = "0") Integer pageNumber,
	                                             @RequestParam(defaultValue = "10") Integer pageSize,
	                                             @RequestParam(required = false) String sortField,
	                                             @RequestParam(required = false) String sortDirection,
	                                             HttpServletRequest request) {

		PageRequest pageRequest = PageRequestHelper.createPageRequest(pageNumber, pageSize,
				StringUtils.isEmpty(sortField) ? null : Collections.singletonList(sortField),
				StringUtils.isEmpty(sortDirection) ? null : Collections.singletonList(sortDirection));
		Page<Product> page = productService.findHiddenProducts(releaseCenterKey, pageRequest);
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(page.getContent(), request, PRODUCT_LINKS.toArray(String[]::new));

		return new PageImpl<>(result, pageRequest, page.getTotalElements());
	}

	@GetMapping( value = "/{productKey}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a product",
			description = "Returns a single product object for a given product key")
	public Map<String, Object> getProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			HttpServletRequest request) {
		Product product = productService.find(releaseCenterKey, productKey, true);
		
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT_ERROR +  productKey);
		}
		
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS.toArray(String[]::new));
	}

	@GetMapping(value = "/manifest/optional-refsets")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns optional manifest refsets",
			description = "Returns the set of optional refsets available for manifest generation.")
	public List<ConceptMini> getOptionalManifestRefsets() {
		List<ConceptMini> optionalRefsets = new ArrayList<>();
		if (!StringUtils.isBlank(optionalManifestRefsetsConfig)) {
			for (String entry : optionalManifestRefsetsConfig.split(",")) {
				String trimmedEntry = entry.trim();
				if (trimmedEntry.isEmpty()) {
					continue;
				}
				String[] parts = trimmedEntry.split("\\|", 2);
				if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
					ConceptMini conceptMini = new ConceptMini();
					conceptMini.setId(parts[0].trim());
					conceptMini.setTerm(parts[1].trim());
					optionalRefsets.add(conceptMini);
				}
			}
		}
		return optionalRefsets;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Create a product",
			description = "Creates a new product with a name as specified in the request and returns the new product object")
	public ResponseEntity<Map<String, Object>> createProduct(@PathVariable String releaseCenterKey,
			@RequestBody Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}

		String name = json.get(ProductService.NAME);
		String overriddenSnomedCtProduct = json.get(ProductService.OVERRIDDEN_SNOMEDCT_PRODUCT);
		Product product = productService.create(releaseCenterKey, name.trim(), overriddenSnomedCtProduct);
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(product, false, request, PRODUCT_LINKS.toArray(String[]::new)), HttpStatus.CREATED);
	}

	@PatchMapping(value = "/{productKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Update a product",
			description = "Updates an existing product with new details and returns updated product")
	public Map<String, Object> updateProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {
		return doUpdateProduct(releaseCenterKey, productKey, json, request);
	}
	
	// Writing clients in Java we find that the standard Java libraries don't support PATCH so, we need
	// a new end point that uses a more common HTTP method.
	// See http://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
	@PutMapping(value = "/{productKey}/configuration", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Update a product",
			description = "Updates an existing product with new details and returns updated product")
	public Map<String, Object> updateProduct2(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody Map<String, String> json, HttpServletRequest request) throws BusinessServiceException {
		return doUpdateProduct(releaseCenterKey, productKey, json, request);
	}

	private Map<String, Object> doUpdateProduct(String releaseCenterKey, String productKey, Map<String, String> json, HttpServletRequest request) throws BusinessServiceException {
		Product product = productService.update(releaseCenterKey, productKey, json);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT_ERROR + productKey);
		}
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS.toArray(String[]::new));
	}


	@PostMapping(value = "/{productKey}/visibility")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@Operation(summary = "Update visibility for product",
			description = "Update an existing product with the visibility flag")
	public ResponseEntity<Void> updateProductVisibility(@PathVariable String releaseCenterKey, @PathVariable String productKey,
													   @RequestParam(required = true, defaultValue = "true") boolean visibility) throws IOException {
		productService.updateVisibility(releaseCenterKey, productKey, visibility);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/{productKey}/manifest/generate",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_XML_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Generate manifest XML",
			description = "Generates a release manifest from the request configuration and returns it as XML")
	public ResponseEntity<String> generateManifest(@PathVariable final String releaseCenterKey,
												   @PathVariable final String productKey,
												   @RequestParam final String branchPath,
												   @RequestBody final ManifestConfig manifestConfig)
			throws ResourceNotFoundException, BusinessServiceException, RestClientException {
		Product product = productService.find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new ResourceNotFoundException(UNABLE_TO_FIND_PRODUCT_ERROR + productKey);
		}
		BuildConfiguration buildConfiguration = product.getBuildConfiguration();
		if (buildConfiguration == null) {
			throw new BadConfigurationException("No configuration for product " + productKey);
		}
		if (buildConfiguration.getEffectiveTime() == null) {
			throw new BadConfigurationException("Effective time has not been configured for product " + productKey);
		}
		if (manifestConfig == null) {
			throw new BadRequestException("Manifest configuration is required.");
		}
		List<String> moduleIds = buildConfiguration.getExtensionConfig() != null ? buildConfiguration.getExtensionConfig().getModuleIdsAsList() : Collections.emptyList();
		String manifestXml = releaseManifestService.generateManifestXml(manifestConfig, releaseCenterKey, branchPath,
				buildConfiguration.getEffectiveTimeSnomedFormat(), buildConfiguration.isDailyBuild(), buildConfiguration.isBetaRelease(), moduleIds);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_XML)
				.body(manifestXml);
	}
}
