package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.controller.BuildController;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.helper.TestEntityFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:servletContext.xml"})
public class HypermediaGeneratorTest {

	@Autowired
	private HypermediaGenerator hypermediaGenerator;

	@Autowired
	private ObjectMapper objectMapper;

	private Build build;
	private MocksControl mocksControl;
	private HttpServletRequest mockServletRequest;

	private static final String REQUEST_URL = "http://localhost/api/v1/centres/international/extensions/snomed_ct_international_edition/products/snomed_ct_international_edition/builds";

	@Before
	public void setup() {
		TestEntityFactory entityFactory = new TestEntityFactory();
		build = entityFactory.createBuild();
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockServletRequest = mocksControl.createMock(HttpServletRequest.class);
	}

	@Test
	public void testName() throws Exception {
		List<Build> builds = new ArrayList<>();
		builds.add(build);

		String expected = FileCopyUtils.copyToString(new InputStreamReader(getClass().getResourceAsStream("expected-build-listing.json")));

		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer(REQUEST_URL)).anyTimes();
		EasyMock.expect(mockServletRequest.getContextPath()).andReturn("api").anyTimes();
		EasyMock.expect(mockServletRequest.getServletPath()).andReturn("/v1").anyTimes();
		mocksControl.replay();

		List<Map<String, Object>> hypermedia = hypermediaGenerator.getEntityCollectionHypermedia(builds, mockServletRequest, BuildController.BUILD_LINKS, "/builds");

		mocksControl.verify();
		Assert.assertNotNull(hypermedia);
		StringWriter stringWriter = new StringWriter();
		objectMapper.writeValue(stringWriter, hypermedia);
		String actual = stringWriter.toString();
		System.out.println(actual);
		Assert.assertEquals(expected, actual);
	}

}
