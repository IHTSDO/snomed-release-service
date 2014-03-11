package org.ihtsdo.buildcloud.controller.helper;

import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.easymock.MockType;
import org.easymock.internal.MocksControl;
import org.ihtsdo.buildcloud.controller.BuildController;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
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
import java.io.IOException;
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
	private Execution execution;
	private MocksControl mocksControl;
	private HttpServletRequest mockServletRequest;

	@Before
	public void setup() {
		TestEntityFactory entityFactory = new TestEntityFactory();
		build = entityFactory.createBuild();
		execution = entityFactory.createExecution();
		mocksControl = new MocksControl(MockType.DEFAULT);
		mockServletRequest = mocksControl.createMock(HttpServletRequest.class);
		EasyMock.expect(mockServletRequest.getContextPath()).andReturn("api").anyTimes();
		EasyMock.expect(mockServletRequest.getServletPath()).andReturn("/v1").anyTimes();
	}

	@Test
	public void testGetEntityCollectionHypermedia() throws Exception {
		List<Build> builds = new ArrayList<>();
		builds.add(build);

		String expected = FileCopyUtils.copyToString(new InputStreamReader(getClass().getResourceAsStream("expected-build-listing.json")));
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/centres/international/extensions/snomed_ct_international_edition/products/snomed_ct_international_edition/builds")).anyTimes();
		mocksControl.replay();

		List<Map<String, Object>> hypermedia = hypermediaGenerator.getEntityCollectionHypermedia(builds, mockServletRequest, BuildController.BUILD_LINKS, "/builds");

		mocksControl.verify();
		Assert.assertNotNull(hypermedia);
		String actual = toString(hypermedia);
		System.out.println(actual);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testLinkNameAndUrl() throws IOException {
		EasyMock.expect(mockServletRequest.getRequestURL()).andReturn(new StringBuffer("http://localhost/api/v1/builds/something/exec/something")).anyTimes();
		mocksControl.replay();

		String linkNameAndUrl = "buildScripts|build-scripts.zip";
		Map<String, Object> hypermedia = hypermediaGenerator.getEntityHypermedia(execution, mockServletRequest, new String[]{"configuration", linkNameAndUrl});

		Assert.assertNotNull(hypermedia);
		String actual = toString(hypermedia);
		System.out.println(actual);
		Assert.assertEquals("http://localhost/api/v1/builds/something/exec/something/build-scripts.zip", hypermedia.get("buildScripts_url"));
	}

	private String toString(Object hypermedia) throws IOException {
		StringWriter stringWriter = new StringWriter();
		objectMapper.writeValue(stringWriter, hypermedia);
		return stringWriter.toString();
	}

}
