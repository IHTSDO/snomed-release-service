package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.dao.s3.TestS3Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JustPackageTestIntegration extends AbstractControllerTest {

	@Autowired
	private S3Client s3Client;

	private IntegrationTestHelper integrationTestHelper;
	
	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"just_package_test");
		((TestS3Client) s3Client).freshBucketStore();
	}

	@Test
	public void testMultipleReleases() throws Exception {
		integrationTestHelper.loginAsManager();
		integrationTestHelper.createTestProductStructure();

		integrationTestHelper.uploadDeltaInputFile("/der2_Refset_SimpleDelta_INT_20140131.txt", getClass());
		integrationTestHelper.uploadManifest("/just_package_manifest_20140131.xml", getClass());
		integrationTestHelper.setEffectiveTime("20140131");
		integrationTestHelper.setJustPackage(true);
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setReadmeHeader("Header");
		String executionURL1 = integrationTestHelper.createExecution();
		integrationTestHelper.triggerExecution(executionURL1);

		integrationTestHelper.publishOutput(executionURL1);

		// Assert first release output expectations
		String expectedZipFilename = "JustPackage_Release_INT_20140131.zip";
		String expectedZipEntries = "JustPackage_Release_INT_20140131/\n" +
				"JustPackage_Release_INT_20140131/RF2Release/\n" +
				"JustPackage_Release_INT_20140131/RF2Release/Delta/\n" +
				"JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/\n" +
				"JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/Content/\n" +
				"JustPackage_Release_INT_20140131/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_20140131.txt";
		integrationTestHelper.testZipNameAndEntryNames(executionURL1, expectedZipFilename, expectedZipEntries, getClass());
	}

}
