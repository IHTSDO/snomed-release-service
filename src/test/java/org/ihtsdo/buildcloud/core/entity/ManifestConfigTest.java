package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestConfigTest {

	@Test
	void getExcludedRf2FilesAsList_shouldParsePipeDelimitedValues() {
		ManifestConfig manifestConfig = new ManifestConfig();
		manifestConfig.setExcludedRf2Files("sct2_StatedRelationship|der2_Refset_Simple| file3 ");

		List<String> excludedFiles = manifestConfig.getExcludedRf2FilesAsList();

		assertEquals(3, excludedFiles.size());
		assertEquals("sct2_StatedRelationship", excludedFiles.get(0));
		assertEquals("der2_Refset_Simple", excludedFiles.get(1));
		assertEquals("file3", excludedFiles.get(2));
	}

	@Test
	void getExcludedRf2FilesAsList_shouldReturnEmptyListWhenUnset() {
		ManifestConfig manifestConfig = new ManifestConfig();

		assertTrue(manifestConfig.getExcludedRf2FilesAsList().isEmpty());
	}

	@Test
	void getIncludedExternalSimpleRefsetsAsList_shouldParseCommaSeparatedValues() {
		ManifestConfig manifestConfig = new ManifestConfig();
		manifestConfig.setIncludedExternalSimpleRefsets("123456789, 987654321 ,");

		List<String> includedRefsets = manifestConfig.getIncludedExternalSimpleRefsetsAsList();

		assertEquals(2, includedRefsets.size());
		assertEquals("123456789", includedRefsets.get(0));
		assertEquals("987654321", includedRefsets.get(1));
	}

	@Test
	void getIncludedExternalSimpleRefsetsAsList_shouldReturnEmptyListWhenUnset() {
		ManifestConfig manifestConfig = new ManifestConfig();

		assertTrue(manifestConfig.getIncludedExternalSimpleRefsetsAsList().isEmpty());
	}

	@Test
	void jacksonRoundTrip_shouldDeserializeIncludedExternalSimpleRefsetsWithoutMutatingImmutableList() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		ManifestConfig original = new ManifestConfig();
		original.setIncludedExternalSimpleRefsets("1,2,3,4,5,6,7,8,9,10");
		original.setExcludedRf2Files("sct2_StatedRelationship|der2_Refset_Simple");
		original.setExcludedRefsets("111,222");

		String json = objectMapper.writeValueAsString(original);
		assertFalse(json.contains("includedExternalSimpleRefsetsAsList"));
		assertFalse(json.contains("excludedRf2FilesAsList"));
		assertFalse(json.contains("excludedRefsetsAsList"));

		// Simulate a legacy SRS message that still embeds the AsList properties.
		String legacyJson = """
				{
				  "includedExternalSimpleRefsets": "1,2,3,4,5,6,7,8,9,10",
				  "includedExternalSimpleRefsetsAsList": ["1","2","3","4","5","6","7","8","9","10"],
				  "excludedRf2Files": "sct2_StatedRelationship|der2_Refset_Simple",
				  "excludedRf2FilesAsList": ["sct2_StatedRelationship","der2_Refset_Simple"],
				  "excludedRefsets": "111,222",
				  "excludedRefsetsAsList": ["111","222"]
				}
				""";

		ManifestConfig roundTripped = objectMapper.readValue(json, ManifestConfig.class);
		ManifestConfig fromLegacy = objectMapper.readValue(legacyJson, ManifestConfig.class);

		assertEquals("1,2,3,4,5,6,7,8,9,10", roundTripped.getIncludedExternalSimpleRefsets());
		assertEquals(10, roundTripped.getIncludedExternalSimpleRefsetsAsList().size());
		assertEquals("1,2,3,4,5,6,7,8,9,10", fromLegacy.getIncludedExternalSimpleRefsets());
		assertEquals(10, fromLegacy.getIncludedExternalSimpleRefsetsAsList().size());
		assertEquals("sct2_StatedRelationship|der2_Refset_Simple", fromLegacy.getExcludedRf2Files());
		assertEquals("111,222", fromLegacy.getExcludedRefsets());
	}

}
