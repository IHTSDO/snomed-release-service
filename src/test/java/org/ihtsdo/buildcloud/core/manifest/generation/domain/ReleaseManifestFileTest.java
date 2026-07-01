package org.ihtsdo.buildcloud.core.manifest.generation.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseManifestFileTest {

	@Test
	void externalOnlyFile_shouldSerializeFileLevelSource() throws Exception {
		ReleaseManifestFile file = new ReleaseManifestFile("der2_Refset_SimpleSnapshot_VET_20260331.txt");
		file.addSource("externally-maintained");
		file.addRefset("123456789", "123456789");

		XmlMapper xmlMapper = new XmlMapper();
		xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		String xml = xmlMapper.writeValueAsString(file);

		assertTrue(xml.contains("externally-maintained"));
		assertFalse(xml.contains("terminology-server"));
		assertTrue(xml.contains("id=\"123456789\""));
	}

	@Test
	void externalAndTerminologyServerFile_shouldSerializeBothFileLevelSources() throws Exception {
		ReleaseManifestFile file = new ReleaseManifestFile("der2_Refset_SimpleSnapshot_VET_20260331.txt");
		file.addSource("terminology-server");
		file.addSource("externally-maintained");
		file.addRefset("123456789", "Test refset");

		XmlMapper xmlMapper = new XmlMapper();
		xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		String xml = xmlMapper.writeValueAsString(file);

		assertTrue(xml.contains("externally-maintained"));
		assertTrue(xml.contains("terminology-server"));
		assertTrue(xml.contains("id=\"123456789\""));
		assertTrue(xml.contains("label=\"Test refset\""));
	}

}
