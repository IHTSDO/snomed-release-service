package org.ihtsdo.buildcloud.entity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BuildConfigurationTest {

	private StringWriter stringWriter;
	private JsonGenerator jsonGenerator;
	private JsonFactory jsonFactory;

	@Before
	public void setUp() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		stringWriter = new StringWriter();
		jsonFactory = objectMapper.getFactory();
		jsonGenerator = jsonFactory.createGenerator(stringWriter);
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
		ExtensionConfig extensionConfig = new ExtensionConfig();
		extensionConfig.setDependencyRelease("SnomedCT_Release_INT_20160131.zip");
		extensionConfig.setModuleId("554471000005108");
		extensionConfig.setNamespaceId("1000005");
		configuration.setExtensionConfig(extensionConfig);

		jsonGenerator.writeObject(configuration);
		String actual = stringWriter.toString();
		System.out.println(actual.replaceAll(",", ",\n"));

		BuildConfiguration buildConfigurationFromJson = jsonFactory.createJsonParser(new StringReader(actual)).readValueAs(BuildConfiguration.class);

		Assert.assertEquals(2, buildConfigurationFromJson.getRefsetCompositeKeys().size());
		Assert.assertEquals(effectiveTime, buildConfigurationFromJson.getEffectiveTime());
		Iterator<BuildConfiguration.RefsetCompositeKey> iterator = getSortedKeys(buildConfigurationFromJson).iterator();
		BuildConfiguration.RefsetCompositeKey key1 = iterator.next();
		Assert.assertEquals("100", key1.getRefsetId());
		Assert.assertEquals("5, 6", key1.getFieldIndexes());
		BuildConfiguration.RefsetCompositeKey key2 = iterator.next();
		Assert.assertEquals("200", key2.getRefsetId());
		Assert.assertEquals("5, 8", key2.getFieldIndexes());
		ExtensionConfig extConfig = buildConfigurationFromJson.getExtensionConfig();
		Assert.assertNotNull(extConfig);
		Assert.assertEquals("SnomedCT_Release_INT_20160131.zip", extConfig.getDependencyRelease());
		Assert.assertEquals("554471000005108", extConfig.getModuleId());
		Assert.assertEquals("1000005", extConfig.getNamespaceId());
	}

	private TreeSet<BuildConfiguration.RefsetCompositeKey> getSortedKeys(BuildConfiguration buildConfigurationFromJson) {
		TreeSet<BuildConfiguration.RefsetCompositeKey> refsetCompositeKeys1 = new TreeSet<>(new Comparator<BuildConfiguration.RefsetCompositeKey>() {
			@Override
			public int compare(BuildConfiguration.RefsetCompositeKey o1, BuildConfiguration.RefsetCompositeKey o2) {
				return o1.getRefsetId().compareTo(o2.getRefsetId());
			}
		});
		refsetCompositeKeys1.addAll(buildConfigurationFromJson.getRefsetCompositeKeys());
		return refsetCompositeKeys1;
	}
}