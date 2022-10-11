package org.ihtsdo.buildcloud.integration.managedservice;

import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.ihtsdo.buildcloud.core.service.PublishService;
import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.util.zip.ZipFile;

import static org.ihtsdo.buildcloud.core.service.ProductServiceImpl.EXTERNAL_DEPENDENCY_PACKAGE_RELEASE_CENTER;

public class ManagedServiceReleaseTestIntegration extends AbstractControllerTest {
	
	private static final String MANAGED_SERVICE_RELEASE = "SnomedCT_ManagedServiceSE1000052_";

	private IntegrationTestHelper integrationTestHelper;

	@Autowired
	private PublishService publishService;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		integrationTestHelper = new IntegrationTestHelper(mockMvc,"ManagedServiceReleaseTestIntegration");
	}

	@Test
	@Ignore
	public void testFirstRelease() throws Exception {
		
		integrationTestHelper.createTestProductStructure();
		
		//config assertion tests
		integrationTestHelper.setAssertionTestConfigProperty(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, "20160731");

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime = "20161130";
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2016");
		Thread.sleep(1000);
		
		loadDeltaFilesToInputDirectory(effectiveTime);

		final String expectedZipEntries =  MANAGED_SERVICE_RELEASE + effectiveTime + "/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Readme_en_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Documentation/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Documentation/Current/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetDelta_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Delta/Terminology/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetFull_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Full/Terminology/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Content/der2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetSnapshot_SE1000052_" + effectiveTime + ".txt\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Language/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Refset/Metadata/\n" +
				 MANAGED_SERVICE_RELEASE + effectiveTime + "/Snapshot/Terminology/";

		// This code extremely sensitive to timing issues because IdAssignmentBIOfflineDemoImpl only has one counter being accessed
		// by multiple streams.
		executeAndVerfiyResults(effectiveTime, expectedZipEntries );
	}

	@Test
	public void testFirstReleaseWithDescriptionsSeparatedByModuleId() throws Exception {
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime = "20161130";
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2016");
		integrationTestHelper.setPreviousPublishedPackage("");
		integrationTestHelper.setAdditionalPreviousPublishedPackages("");
		Thread.sleep(1000);

		integrationTestHelper.uploadManifest("ch_manifest.xml", getClass());

		final String buildURL = integrationTestHelper.createBuild(effectiveTime);
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_CH1000195_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_CH1000195_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-de-ch_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-fr_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-fr-ch_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-it-ch_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.getInputFile("rel2_Description_Delta-it-ch_CH1000195_" + effectiveTime +".txt");
		integrationTestHelper.scheduleBuild(buildURL);
		integrationTestHelper.waitUntilCompleted(buildURL);

		String CH_RELEASE = "SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_";
		final String expectedZipEntries =  CH_RELEASE + effectiveTime + "/\n" +
				CH_RELEASE + effectiveTime + "/Readme_ch_" + effectiveTime + ".txt\n" +
				CH_RELEASE + effectiveTime + "/Delta/\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/sct2_Description_Delta-en_CH1000195_20161130.txt\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/sct2_Description_Delta-de-ch_CH1000195_20161130.txt\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/sct2_Description_Delta-fr_CH1000195_20161130.txt\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/sct2_Description_Delta-fr-ch_CH1000195_20161130.txt\n" +
				CH_RELEASE + effectiveTime + "/Delta/Terminology/sct2_Description_Delta-it-ch_CH1000195_20161130.txt";

		// Assert first release output expectations
		final String expectedZipFilename = "SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_" + effectiveTime + ".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), false);
	}

	@Test
	public void testReleaseWithMultiplePreviousReleases() throws Exception {
		integrationTestHelper.createTestProductStructure();

		// Perform first time release
		integrationTestHelper.setFirstTimeRelease(true);
		integrationTestHelper.setCreateLegacyIds(true);
		final String effectiveTime = "20161130";
		integrationTestHelper.setEffectiveTime(effectiveTime);
		integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setReadmeEndDate("2016");
		Thread.sleep(1000);

		integrationTestHelper.uploadManifest("ch_manifest_20161130.xml", getClass());

		String buildURL = integrationTestHelper.createBuild(effectiveTime);
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_CH1000195_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_CH1000195_" + effectiveTime + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_CH1000195_" + effectiveTime +".txt", getClass());
		integrationTestHelper.scheduleBuild(buildURL);
		integrationTestHelper.waitUntilCompleted(buildURL);
		integrationTestHelper.publishOutput(buildURL);
		Thread.sleep(1000);

		integrationTestHelper.createReleaseCenter("External Dependency Packages");
		integrationTestHelper.publishFile("/SnomedCT_CommonFrench_PRODUCTION_CH1000195_20170330.zip", getClass(), HttpStatus.CREATED, "/centers/" + "external_dependency_packages");

		// perform second time release for Swiss extension
		integrationTestHelper.createTestProductStructure();
		integrationTestHelper.setFirstTimeRelease(false);

		final String effectiveTime2 = "20171130";
		integrationTestHelper.setEffectiveTime(effectiveTime2);
		integrationTestHelper.setBetaRelease(false);
		integrationTestHelper.setReadmeEndDate("2017");
		integrationTestHelper.setReadmeHeader("This is the readme for the second release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
		integrationTestHelper.setPreviousPublishedPackage("SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_20161130.zip");
		integrationTestHelper.setAdditionalPreviousPublishedPackages("SnomedCT_CommonFrench_PRODUCTION_CH1000195_20170330.zip");
		Thread.sleep(1000);

		integrationTestHelper.uploadManifest("ch_manifest_20171130.xml", getClass());

		buildURL = integrationTestHelper.createBuild(effectiveTime2);
		integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_CH1000195_" + effectiveTime2 + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_CH1000195_" + effectiveTime2 + ".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_CH1000195_" + effectiveTime2 +".txt", getClass());
		integrationTestHelper.scheduleBuild(buildURL);
		integrationTestHelper.waitUntilCompleted(buildURL);

		String CH_RELEASE = "SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_";
		final String expectedZipEntries =  CH_RELEASE + effectiveTime2 + "/\n" +
				CH_RELEASE + effectiveTime2 + "/Readme_ch_" + effectiveTime2 + ".txt\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/Terminology/\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/Terminology/sct2_Description_Snapshot-en_CH1000195_20171130.txt\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/Refset/\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/Refset/Language/\n" +
				CH_RELEASE + effectiveTime2 + "/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot_fr-CH_20171130.txt\n" +
				CH_RELEASE + effectiveTime2 + "/Full/\n" +
				CH_RELEASE + effectiveTime2 + "/Full/Terminology/\n" +
				CH_RELEASE + effectiveTime2 + "/Full/Terminology/sct2_Description_Full-en_CH1000195_20171130.txt\n" +
				CH_RELEASE + effectiveTime2 + "/Full/Refset/\n" +
				CH_RELEASE + effectiveTime2 + "/Full/Refset/Language/\n" +
				CH_RELEASE + effectiveTime2 + "/Full/Refset/Language/der2_cRefset_LanguageFull_fr-CH_20171130.txt";

		// Assert second release output expectations including additional releases
		final String expectedZipFilename = "SnomedCT_ManagedServiceCH_PRODUCTION_CH1000195_" + effectiveTime2 + ".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), false);
	}
	
	
	private void loadDeltaFilesToInputDirectory(final String releaseDate) throws Exception {
		integrationTestHelper.uploadManifest("se_manifest.xml", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalDeltagandetyperSocialtjänstSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		integrationTestHelper.uploadDeltaInputFile("rel2_Refset_UrvalSambandstyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_" + releaseDate +".txt", getClass());
		String inputFileName = "rel2_Refset_UrvalSambandstyperSocialtjänstSimpleRefsetDelta_SE1000052_" + releaseDate +".txt";
		integrationTestHelper.uploadDeltaInputFile(inputFileName, getClass());
		integrationTestHelper.getInputFile(inputFileName);
	}
	
	

	private void executeAndVerfiyResults(final String releaseDate, final String expectedZipEntries) throws Exception {
		final String buildURL1 = integrationTestHelper.createBuild(releaseDate);
		integrationTestHelper.scheduleBuild(buildURL1);
		integrationTestHelper.waitUntilCompleted(buildURL1);
		integrationTestHelper.publishOutput(buildURL1);

		// Assert first release output expectations
		final String expectedZipFilename = "SnomedCT_ManagedServiceSE1000052_" + releaseDate + ".zip";
		final ZipFile zipFile = integrationTestHelper.testZipNameAndEntryNames(buildURL1, expectedZipFilename, expectedZipEntries, getClass());
		integrationTestHelper.assertZipContents("expectedoutput", zipFile, getClass(), false);
	}

}
