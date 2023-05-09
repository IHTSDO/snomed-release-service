package org.ihtsdo.buildcloud.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.buildcloud.rest.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.rest.controller.helper.PageRequestHelper;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead;
import org.ihtsdo.buildcloud.rest.security.IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.helper.FilterOption;
import org.ihtsdo.otf.rest.exception.BadRequestException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@ConditionalOnProperty(name = "srs.manager", havingValue = "true")
@Controller
@RequestMapping("/centers/{releaseCenterKey}/products")
@Tag(name = "Product", description = "-")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] PRODUCT_LINKS = {"manifest", "builds"};

	@GetMapping
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a list of products",
			description = "Returns a list of products for the extension specified in the URL")
	@ResponseBody
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
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(page.getContent(), request, PRODUCT_LINKS);

		return new PageImpl<>(result, pageRequest, page.getTotalElements());
	}

	@GetMapping( value = "/hidden")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a list of  hidden products",
			description = "Returns a list of hidden products for the extension specified in the URL")
	@ResponseBody
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
		List<Map<String, Object>> result = hypermediaGenerator.getEntityCollectionHypermedia(page.getContent(), request, PRODUCT_LINKS);

		return new PageImpl<>(result, pageRequest, page.getTotalElements());
	}

	@GetMapping( value = "/{productKey}")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLeadOrUser
	@Operation(summary = "Returns a product",
			description = "Returns a single product object for a given product key")
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
	@Operation(summary = "Create a product",
			description = "Creates a new product with a name as specified in the request and returns the new product object")
	public ResponseEntity<Map<String, Object>> createProduct(@PathVariable String releaseCenterKey,
			@RequestBody Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}

		String name = json.get(ProductService.NAME);
		Product product = productService.create(releaseCenterKey, name);
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(product, false, request, ProductController.PRODUCT_LINKS), HttpStatus.CREATED);
	}

	@PatchMapping(value = "/{productKey}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Update a product",
			description = "Updates an existing product with new details and returns updated product")
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
	@Operation(summary = "Update a product",
			description = "Updates an existing product with new details and returns updated product")
	public Map<String, Object> updateProduct2(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody Map<String, String> json, HttpServletRequest request) throws BusinessServiceException {
		Product product = productService.update(releaseCenterKey, productKey, json);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " + productKey);
		}
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}


	@PostMapping(value = "/{productKey}/visibility")
	@IsAuthenticatedAsAdminOrReleaseManagerOrReleaseLead
	@ResponseBody
	@Operation(summary = "Update visibility for product",
			description = "Update an existing product with the visibility flag")
	public ResponseEntity updateProductVisibility(@PathVariable String releaseCenterKey, @PathVariable String productKey,
													   @RequestParam(required = true, defaultValue = "true") boolean visibility) {
		productService.updateVisibility(releaseCenterKey, productKey, visibility);
		return new ResponseEntity(HttpStatus.OK);
	}
}
