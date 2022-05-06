package org.ihtsdo.buildcloud.core.manifest;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class ManifestValidatorTest {

	@Test
	public void testManifestWithWrongSourcesConfig() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config.xml");
		assertNotNull(manifestStream);
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNotNull(validationMsg);
		assertEquals("cvc-complex-type.2.4.a: Invalid content was found" +
				" starting with element '{\"http://release.ihtsdo.org/manifest/1.0.0\":sources}'." +
				" One of '{\"http://release.ihtsdo.org/manifest/1.0.0\":refset}' is expected. " +
				"The issue lies in the manifest.xml at line 8 and column 17", validationMsg);
	}

	@Test
	public void testManifestWithWrongSourcesConfigInFileTag() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config_in_fileTag.xml");
		assertNotNull(manifestStream);
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNotNull(validationMsg);
		assertEquals("cvc-complex-type.2.4.a: Invalid content was found starting with element '{" +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":source}'. One of '{" +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":contains-reference-sets, " +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":contains-module-ids, " +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":contains-language-codes, " +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":contains-additional-fields, " +
				"\"http://release.ihtsdo.org/manifest/1.0.0\":sources}' is expected. The issue lies in the manifest.xml at line 7 and column 15", validationMsg);
	}

	@Test
	public void testManifestWithWrongSourcesConfigInRefsetTag() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config_in_refsetTag.xml");
		assertNotNull(manifestStream);
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNotNull(validationMsg);
		assertEquals("cvc-complex-type.2.4.a: Invalid content was found starting with element " +
				"'{\"http://release.ihtsdo.org/manifest/1.0.0\":source}'. One of '{\"http://release.ihtsdo.org/manifest/1.0.0\":sources}' " +
				"is expected. The issue lies in the manifest.xml at line 14 and column 17", validationMsg);
	}

	@Test
	public void testBasicManifestWithDeltaOnly() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_basic_with_delta_only.xml");
		assertNotNull(manifestStream);
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNull(validationMsg);
	}

	@Test
	public void testBasicManifest() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_basic.xml");
		assertNotNull(manifestStream);
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNull(validationMsg);
	}
}
