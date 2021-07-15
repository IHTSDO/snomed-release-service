package org.ihtsdo.buildcloud.rest.controller.helper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.TestConfig;
import org.ihtsdo.buildcloud.rest.controller.ProductController;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.entity.QATestConfig;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityFactory;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.FileCopyUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
public class HypermediaGeneratorTest {

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private ObjectMapper objectMapper;

	private Product product;
	private Build build;
	private MocksControl mocksControl;
	private HttpServletRequest mockServletRequest;

	@Before
	public void setup() {
		final TestEntityFactory entityFactory = new TestEntityFactory();
		product = entityFactory.createProduct();
		final BuildConfiguration buildConfig = new BuildConfiguration();
		product.setBuildConfiguration(buildConfig);
		final QATestConfig qaConfig = new QATestConfig();
		product.setQaTestConfig(qaConfig);
		build = entityFactory.createBuild();
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockServletRequest = mocksControl.createMock(HttpServletRequest.class);
		EasyMock.expect(mockServletRequest.getContextPath()).andReturn("api").anyTimes();
		EasyMock.expect(mockServletRequest.getServletPath()).andReturn("/v1").anyTimes();
	}

	@Test
	public void testGetEntityCollectionHypermedia() throws Exception {
		final List<Product> products = new ArrayList<>();
		products.add(product);

		String expected = FileCopyUtils.copyToString(new InputStreamReader(getClass().getResourceAsStream("expected-product-listing.json"), RF2Constants.UTF_8));
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/centers/international/extensions/snomed_ct_international_edition/products/snomed_ct_international_edition/products")).anyTimes();
		mocksControl.replay();

		final List<Map<String, Object>> hypermedia = hypermediaGenerator.getEntityCollectionHypermedia(products, mockServletRequest, ProductController.PRODUCT_LINKS, "/products");

		mocksControl.verify();
		Assert.assertNotNull(hypermedia);
		String actual = toString(hypermedia);
		expected = expected.replaceAll("\r\n", "/n");
		actual = actual.replaceAll("/r/n","/n");
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testLinkNameAndUrl() throws IOException {
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/products/something/exec/something")).anyTimes();
		mocksControl.replay();

		final String linkNameAndUrl = "productScripts|product-scripts.zip";
		final boolean currentResource = true;
		final Map<String, Object> hypermedia = hypermediaGenerator.getEntityHypermedia(build, currentResource, mockServletRequest, "configuration", linkNameAndUrl);

		Assert.assertNotNull(hypermedia);
		System.out.println(toString(hypermedia));
		Assert.assertEquals("http://localhost/api/v1/products/something/exec/something/product-scripts.zip", hypermedia.get("productScripts_url"));
	}

	@Test
	public void testActionResponseUrl() throws IOException {
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/products/something/exec/something/trigger")).anyTimes();
		mocksControl.replay();

		final Map<String, Object> hypermedia = hypermediaGenerator.getEntityHypermediaOfAction(build, mockServletRequest, "configuration", "productScripts|product-scripts.zip");

		Assert.assertNotNull(hypermedia);
		System.out.println(toString(hypermedia));
		Assert.assertEquals("http://localhost/api/v1/products/something/exec/something", hypermedia.get("url"));
		Assert.assertEquals("http://localhost/api/v1/products/something/exec/something/product-scripts.zip", hypermedia.get("productScripts_url"));
	}

	private String toString(final Object hypermedia) throws IOException {
		final StringWriter stringWriter = new StringWriter();
		objectMapper.writeValue(stringWriter, hypermedia);
		return stringWriter.toString();
	}
}
