package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centers/{releaseCenterBusinessKey}/extensions/{extensionBusinessKey}/products")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PRODUCT_LINKS = {"builds"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getProducts(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) throws Exception{
		//TODO PETER - Return 404 rather than throw exception if extension not found.
		String authenticatedId = SecurityHelper.getSubject();
		List<Product> products = productService.findAll(releaseCenterBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(products, request, PRODUCT_LINKS);
	}

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map> createProduct(@PathVariable String releaseCenterBusinessKey,
											 @PathVariable String extensionBusinessKey,
											 @RequestBody(required = false) Map<String, String> json,
												   HttpServletRequest request) throws IOException {

		String name = json.get("name");

		String authenticatedId = SecurityHelper.getSubject();
		Product product = productService.create(releaseCenterBusinessKey, extensionBusinessKey, name, authenticatedId);
		Map<String, Object> entityHypermedia = hypermediaGenerator.getEntityHypermediaJustCreated(product, request, PRODUCT_LINKS);
		return new ResponseEntity<Map>(entityHypermedia, HttpStatus.CREATED);
	}

	@RequestMapping("/{productBusinessKey}")
	@ResponseBody
	public Map getProduct(@PathVariable String releaseCenterBusinessKey, @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Product product = productService.find(releaseCenterBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(product, request, PRODUCT_LINKS);
	}

}
