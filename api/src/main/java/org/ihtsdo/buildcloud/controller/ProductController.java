package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.ProductService;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.BusinessServiceException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/centers/{releaseCenterKey}/products")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	public static final String[] PRODUCT_LINKS = {"executions"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getProducts(@PathVariable String releaseCenterKey, @RequestParam(required = false) boolean includeRemoved,
			HttpServletRequest request) {
		
		Set<FilterOption> filterOptions = EnumSet.noneOf(FilterOption.class);
		if (includeRemoved) {
			filterOptions.add(FilterOption.INCLUDE_REMOVED);
		}

		List<Product> products = productService.findAll(releaseCenterKey, filterOptions);
		return hypermediaGenerator.getEntityCollectionHypermedia(products, request, PRODUCT_LINKS);
	}

	@RequestMapping("/{productKey}")
	@ResponseBody
	public Map<String, Object> getProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			HttpServletRequest request) throws BusinessServiceException {
		Product product = productService.find(releaseCenterKey, productKey);
		
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " +  productKey);
		}
		
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createProduct(@PathVariable String releaseCenterKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		if (json == null) {
			throw new BadRequestException("No JSON payload in request body.");
		}

		String name = json.get(ProductService.NAME);
		Product product = productService.create(releaseCenterKey, name);

		boolean currentResource = false;
		return new ResponseEntity<>(hypermediaGenerator.getEntityHypermedia(product, currentResource, request, ProductController.PRODUCT_LINKS), HttpStatus.CREATED);
	}

	@RequestMapping(value = "/{productKey}", method = RequestMethod.PATCH, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public Map<String, Object> updateProduct(@PathVariable String releaseCenterKey, @PathVariable String productKey,
			@RequestBody(required = false) Map<String, String> json,
			HttpServletRequest request) throws BusinessServiceException {

		Product product = productService.update(releaseCenterKey, productKey, json);
		if (product == null) {
			throw new ResourceNotFoundException("Unable to find product: " +  productKey);
		}
		return hypermediaGenerator.getEntityHypermedia(product, true, request, PRODUCT_LINKS);
	}
	
}
