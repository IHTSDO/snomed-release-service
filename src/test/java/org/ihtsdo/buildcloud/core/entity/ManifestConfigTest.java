package org.ihtsdo.buildcloud.core.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
