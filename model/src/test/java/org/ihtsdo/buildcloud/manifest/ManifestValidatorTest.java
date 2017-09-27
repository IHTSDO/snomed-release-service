package org.ihtsdo.buildcloud.manifest;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;

public class ManifestValidatorTest {
	
	@Test
	public void testManifestWithWrongSourcesConfig() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config.xml");
		String validationMsg = ManifestValidator.validate(manifestStream);
		if (validationMsg != null) {
			assertEquals("cvc-complex-type.2.4.a: Invalid content was found starting with element 'sources'. "
					+ "One of '{\"http://release.ihtsdo.org/manifest/1.0.0\":refset}' is expected. "
					+ "The issue lies in the manifest.xml at line 8 and column 17",validationMsg);
		}
	}
	
	@Test
	public void testManifestWithWrongSourcesConfigInFileTag() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config_in_fileTag.xml");
		String validationMsg = ManifestValidator.validate(manifestStream);
		if (validationMsg != null) {
			assertEquals("cvc-complex-type.2.4.a: Invalid content was found starting with element 'source'. "
					+ "One of '{\"http://release.ihtsdo.org/manifest/1.0.0\":sources}' is expected. "
					+ "The issue lies in the manifest.xml at line 14 and column 16", validationMsg);
		}
	}
	
	@Test
	public void testManifestWithWrongSourcesConfigInRefsetTag() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_with_wrong_sources_config_in_refsetTag.xml");
		String validationMsg = ManifestValidator.validate(manifestStream);
		if (validationMsg != null) {
			assertEquals("cvc-complex-type.2.4.a: Invalid content was found starting with element 'source'. "
					+ "One of '{\"http://release.ihtsdo.org/manifest/1.0.0\":sources}' is expected. "
					+ "The issue lies in the manifest.xml at line 14 and column 17", validationMsg);
		}
	}
	
	@Test
	public void testBasicManifestWithDeltaOnly() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_basic_with_delta_only.xml");
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNull(validationMsg);
	}

	@Test
	public void testBasicManifest() {
		InputStream manifestStream = getClass().getResourceAsStream("manifest_basic.xml");
		String validationMsg = ManifestValidator.validate(manifestStream);
		assertNull(validationMsg);
	}
}
