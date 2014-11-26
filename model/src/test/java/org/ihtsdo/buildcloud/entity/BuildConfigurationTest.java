package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;

public class BuildConfigurationTest {

	private StringWriter stringWriter;
	private JsonGenerator jsonGenerator;
	private JsonFactory jsonFactory;

	@Before
	public void setUp() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		stringWriter = new StringWriter();
		jsonFactory = objectMapper.getJsonFactory();
		jsonGenerator = jsonFactory.createJsonGenerator(stringWriter);
	}

	@Test
	public void testToJsonAndBack() throws IOException {
		BuildConfiguration configuration = new BuildConfiguration();

		Date effectiveTime = new GregorianCalendar(2014, 6, 31).getTime();
		configuration.setEffectiveTime(effectiveTime);

		HashSet<BuildConfiguration.RefsetCompositeKey> refsetCompositeKeys = new HashSet<>();
		refsetCompositeKeys.add(new BuildConfiguration.RefsetCompositeKey("100", "5, 6"));
		refsetCompositeKeys.add(new BuildConfiguration.RefsetCompositeKey("200", "5, 8"));
		configuration.setRefsetCompositeKeys(refsetCompositeKeys);

		jsonGenerator.writeObject(configuration);
		String actual = stringWriter.toString();
		System.out.println(actual.replaceAll(",", ",\n"));

		BuildConfiguration buildConfigurationFromJson = jsonFactory.createJsonParser(new StringReader(actual)).readValueAs(BuildConfiguration.class);

		Assert.assertEquals(2, buildConfigurationFromJson.getRefsetCompositeKeys().size());
		Assert.assertEquals(effectiveTime, buildConfigurationFromJson.getEffectiveTime());
		Iterator<BuildConfiguration.RefsetCompositeKey> iterator = buildConfigurationFromJson.getRefsetCompositeKeys().iterator();
		BuildConfiguration.RefsetCompositeKey key1 = iterator.next();
		Assert.assertEquals("200", key1.getRefsetId());
		Assert.assertEquals("5, 8", key1.getFieldIndexes());
		BuildConfiguration.RefsetCompositeKey key2 = iterator.next();
		Assert.assertEquals("100", key2.getRefsetId());
		Assert.assertEquals("5, 6", key2.getFieldIndexes());
	}
}