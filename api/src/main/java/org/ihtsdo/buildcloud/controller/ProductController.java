package org.ihtsdo.buildcloud.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.service.CreateReleasePackageBuildRequest;
import org.ihtsdo.buildcloud.service.ProductService;
import org.ihtsdo.buildcloud.service.ReleaseService;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products")
@Api(value = "Product", position = 2)
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private ReleaseService releaseService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] PRODUCT_LINKS = {"manifest", "inputfiles","sourcefiles","builds","buildLogs"};

	@GetMapping
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a list of products",
	notes = "Returns a list of products for the extension specified in the URL" )
	@ResponseBody
	public Page<Map<String, Object>> getProducts(@PathVariable String releaseCenterKey,
												 @RequestParam(required = false) boolean includeRemoved,
												 @RequestParam(required = false) boolean includeLegacy,
												 @RequestParam(defaultValue = "0") Integer pageNumber,
												 @RequestParam(defaultValue = "10") Integer pageSize,
												 HttpServletRequest request) {
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (includeRemoved) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}
		if (includeLegacy) {
			filterOptions.add(FilterOption.INCLUDE_LEGACY);
		}


		Page<Product> page = productService.findAll(releaseCenterKey, filterOptions, PageRequest.of(pageNumber, pageSize));
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(page.getContent(), request, PRODUCT_LINKS);

		return new PageImpl<>(result, PageRequest.of(pageNumber, pageSize), page.getTotalElements());
	}

	@GetMapping( value = "/{productKey}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@ApiOperation( value = "Returns a product", notes = "Returns a single product object for a given product key" )
	@ResponseBody
	public Map<String, Object> getProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			HttpServletRequest request) throws BusinessServiceException {
		Product product = productService.find(releaseCenterKey, productKey, true);
		
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " +  productKey);
		}
		
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ApiOperation( value = "Create a product",
		notes = "creates a new Product with a name as specified in  the request "
				+ "and returns the new product object" )
	public ResponseEntity<Map<String, Object>> createProduct(@PathVariable String releaseCenterKey,
			@RequestBody Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}

		String name = json.get(ProductService.NAME);
		Product product = productService.create(releaseCenterKey, name);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(product, currentResource, request, ProductController.PRODUCT_LINKS), HttpStatus.CREATED);
	}

	@PatchMapping(value = "/{productKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Update a product", notes = "Update an existing product with new details "
			+ "and returns updated product" )
	public Map<String, Object> updateProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		Product product = productService.update(releaseCenterKey, productKey, json);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " +  productKey);
		}
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}
	
	// Writing clients in Java we find that the standard Java libraries don't support PATCH so, we need
	// a new end point that uses a more common HTTP method.
	// See http://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
	@PutMapping(value = "/{productKey}/configuration", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation(value = "Update a product", notes = "Update an existing product with new details " + "and returns updated product")
	public Map<String, Object> updateProduct2(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody Map<String, String> json, HttpServletRequest request) throws BusinessServiceException {
		Product product = productService.update(releaseCenterKey, productKey, json);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}

	@PostMapping(value = "/{productKey}/release", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation(value = "Create a release package", notes = "Automatically gather, process input files and make a new build")
	public ResponseEntity createReleasePackage(
			@PathVariable final String releaseCenterKey,
			@PathVariable final String productKey,
			@RequestBody final GatherInputRequestPojo buildConfig,
			final HttpServletRequest request) throws BusinessServiceException {
		final Build newBuild = releaseService.createBuild(releaseCenterKey, productKey, buildConfig, SecurityUtil.getUsername());
		releaseService.queueBuild(new CreateReleasePackageBuildRequest(newBuild,
				buildConfig, hypermediaGenerator.getRootURL(request), SecurityUtil.getUsername(), SecurityUtil.getAuthenticationToken()));
		return new ResponseEntity<>(newBuild, HttpStatus.CREATED);
	}

	@PostMapping(value = "/{productKey}/release/clear-concurrent-cache", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation(value = "Clear in-memory concurrent cache")
	public ResponseEntity clearConcurrentCache(@PathVariable String releaseCenterKey, @PathVariable String productKey) throws BusinessServiceException {
		releaseService.clearConcurrentCache(releaseCenterKey, productKey);
		return new ResponseEntity(HttpStatus.OK);
	}

	@PostMapping(value = "/{productKey}/visibility")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@ApiOperation( value = "Update visibility for product", notes = "Update an existing product with the visibility flag")
	public ResponseEntity updateProductVisibility(@PathVariable String releaseCenterKey, @PathVariable String productKey,
													   @RequestParam(required = true, defaultValue = "true") boolean visibility) throws BusinessServiceException {
		productService.updateVisibility(releaseCenterKey, productKey, visibility);
		return new ResponseEntity(HttpStatus.OK);
	}
}
