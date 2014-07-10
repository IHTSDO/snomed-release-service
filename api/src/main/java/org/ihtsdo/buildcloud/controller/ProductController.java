package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.ihtsdo.buildcloud.service.PublishService;
import org.ihtsdo.buildcloud.service.exception.BadRequestException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers/{releaseCenterBusinessKey}/extensions/{extensionBusinessKey}/products")
public class ProductController {

	@Autowired
	private ProductService productService;
	
	@Autowired
	private PublishService publishService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PRODUCT_LINKS = {"builds", "published"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getProducts(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) throws Exception{
		User authenticatedUser = SecurityHelper.getSubject();
		List<Product> products = productService.findAll(releaseCenterBusinessKey, extensionBusinessKey, authenticatedUser);
		return hypermediaGenerator.getEntityCollectionHypermedia(products, request, PRODUCT_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> createProduct(@PathVariable String releaseCenterBusinessKey,
											 @PathVariable String extensionBusinessKey,
											 @RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws IOException, ResourceNotFoundException {

		String name = json.get("name");
		User authenticatedUser = SecurityHelper.getSubject();
		Product product = productService.create(releaseCenterBusinessKey, extensionBusinessKey, name, authenticatedUser);

		boolean currentResource = true;
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermedia(product, currentResource, request, PRODUCT_LINKS);

		return new ResponseEntity<Map<String, Object>>(entityHypermedia, HttpStatus.CREATED);
	}

	@RequestMapping("/{productBusinessKey}")
	@ResponseBody
	public Map<String, Object> getProduct(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		User authenticatedUser = SecurityHelper.getSubject();
		Product product = productService.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser);
		
		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException ("Unable to find product: " +  item);
		}

		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(product, currentResource, request, PRODUCT_LINKS);

	}
	
	@RequestMapping("/{productBusinessKey}/published")
	@ResponseBody
	public Map<String, Object> getPublishedPackages(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey, HttpServletRequest request) throws ResourceNotFoundException {

		User authenticatedUser = SecurityHelper.getSubject();  
		Product product = productService.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedUser);
		
		if (product == null) {
			String item = CompositeKeyHelper.getPath(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey);
			throw new ResourceNotFoundException ("Unable to find product: " +  item);
		}
		
		List<String> publishedPackages = publishService.getPublishedPackages(product);
		Map<String, Object> jsonStructure = new HashMap<>();
		jsonStructure.put("publishedPackages", publishedPackages);
		boolean currentResource = true;
		return hypermediaGenerator.getEntityHypermedia(jsonStructure, currentResource, request);
	}
	
	
	@RequestMapping(value = "/{productBusinessKey}/published", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Void> uploadInputFileFile(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey,
											 @RequestParam(value = "file") MultipartFile file) throws IOException, ResourceNotFoundException, BadRequestException {

		publishService.publishPackage(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, file.getInputStream(), file.getOriginalFilename(), file.getSize(), SecurityHelper.getSubject());
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}


}
