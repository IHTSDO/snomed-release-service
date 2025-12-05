package org.ihtsdo.buildcloud.rest.controller.helper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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
import org.ihtsdo.buildcloud.test.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.FileCopyUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
class HypermediaGeneratorTest extends AbstractTest {

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private ObjectMapper objectMapper;

	private Product product;
	private Build build;
	private MocksControl mocksControl;
	private HttpServletRequest mockServletRequest;

	@BeforeEach
    @Override
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
    void testGetEntityCollectionHypermedia() throws Exception {
		final List<Product> products = new ArrayList<>();
		products.add(product);

		String expected = FileCopyUtils.copyToString(new InputStreamReader(getClass().getResourceAsStream("expected-product-listing.json"), RF2Constants.UTF_8));
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/centers/international/extensions/snomed_ct_international_edition/products/snomed_ct_international_edition/products")).anyTimes();
		mocksControl.replay();

		final List<Map<String, Object>> hypermedia = hypermediaGenerator.getEntityCollectionHypermedia(products, mockServletRequest, ProductController.PRODUCT_LINKS, "/products");

		mocksControl.verify();
		assertNotNull(hypermedia);
		String actual = toString(hypermedia);
		expected = expected.replaceAll("\r\n", "/n");
		actual = actual.replaceAll("/r/n","/n");
		assertEquals(expected, actual);
	}

	@Test
    void testLinkNameAndUrl() throws IOException {
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/products/something/exec/something")).anyTimes();
		mocksControl.replay();

		final String linkNameAndUrl = "productScripts|product-scripts.zip";
		final boolean currentResource = true;
		final Map<String, Object> hypermedia = hypermediaGenerator.getEntityHypermedia(build, currentResource, mockServletRequest, "configuration", linkNameAndUrl);

		assertNotNull(hypermedia);
		System.out.println(toString(hypermedia));
		assertEquals("http://localhost/api/v1/products/something/exec/something/product-scripts.zip", hypermedia.get("productScripts_url"));
	}

	@Test
    void testActionResponseUrl() throws IOException {
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/products/something/exec/something/trigger")).anyTimes();
		mocksControl.replay();

		final Map<String, Object> hypermedia = hypermediaGenerator.getEntityHypermediaOfAction(build, mockServletRequest, "configuration", "productScripts|product-scripts.zip");

		assertNotNull(hypermedia);
		System.out.println(toString(hypermedia));
		assertEquals("http://localhost/api/v1/products/something/exec/something", hypermedia.get("url"));
		assertEquals("http://localhost/api/v1/products/something/exec/something/product-scripts.zip", hypermedia.get("productScripts_url"));
	}

	private String toString(final Object hypermedia) throws IOException {
		final StringWriter stringWriter = new StringWriter();
		objectMapper.writeValue(stringWriter, hypermedia);
		return stringWriter.toString();
	}
}
