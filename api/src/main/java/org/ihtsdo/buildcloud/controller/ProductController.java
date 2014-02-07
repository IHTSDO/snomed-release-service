package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.security.SecurityHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/centres/{releaseCentreBusinessKey}/extensions/{extensionBusinessKey}/products")
public class ProductController {

	@Autowired
	private ProductService productService;

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	private static final String[] PRODUCT_LINKS = {"builds"};

	@RequestMapping
	@ResponseBody
	public List<Map<String, Object>> getProducts(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		List<Product> products = productService.findAll(releaseCentreBusinessKey, extensionBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityCollectionHypermedia(products, request, PRODUCT_LINKS);
	}

	@RequestMapping("/{productBusinessKey}")
	@ResponseBody
	public Map getProduct(@PathVariable String releaseCentreBusinessKey, @PathVariable String extensionBusinessKey, @PathVariable String productBusinessKey, HttpServletRequest request) {
		String authenticatedId = SecurityHelper.getSubject();
		Product product = productService.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, authenticatedId);
		return hypermediaGenerator.getEntityHypermedia(product, request, PRODUCT_LINKS);
	}

}
