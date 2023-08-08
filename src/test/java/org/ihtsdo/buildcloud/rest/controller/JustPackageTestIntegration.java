package org.ihtsdo.buildcloud.rest.controller;

import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JustPackageTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	private IntegrationTestHelper integrationTestHelper;
	
	@Override
	@BeforeEach
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"just_package_test");
	}

	@Test
	public void testMultipleReleases() throws Exception {
		integrationTestHelper.createTestProductStructure();

		integrationTestHelper.uploadManifest("/just_package_manifest_20140131.xml", getClass());
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setJustPackage(true);
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setReadmeHeader("Header");

		String buildURL = integrationTestHelper.createBuild("20140131");
		integrationTestHelper.uploadDeltaInputFile("/der2_Refset_SimpleDelta_INT_20140131.txt", getClass());
		integrationTestHelper.scheduleBuild(buildURL);
		integrationTestHelper.waitUntilCompleted(buildURL);

		integrationTestHelper.publishOutput(buildURL);

		// Assert first release output expectations
		String expectedZipFilename = "JustPackage_Release_INT_20140131.zip";
		String expectedZipEntries = """
                JustPackage_Release_INT_20140131/
                JustPackage_Release_INT_20140131/RF2Release/
                JustPackage_Release_INT_20140131/RF2Release/Delta/
                JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/
                JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/Content/
                JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt""";
		integrationTestHelper.testZipNameAndEntryNames(buildURL, expectedZipFilename, expectedZipEntries, getClass());
	}

}
