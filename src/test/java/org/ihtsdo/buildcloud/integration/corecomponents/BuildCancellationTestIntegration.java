package org.ihtsdo.buildcloud.integration.corecomponents;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.service.ProductService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BuildCancellationTestIntegration extends AbstractControllerTest{

    private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

    private IntegrationTestHelper integrationTestHelper;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        integrationTestHelper = new IntegrationTestHelper(mockMvc,"CoreComponentsTest");
    }

    @Test
    @Ignore
    public void testCancelBuild() throws Exception {
        integrationTestHelper.createTestProductStructure();

        //config assertion tests
        integrationTestHelper.setAssertionTestConfigProperty(ProductService.ASSERTION_GROUP_NAMES, "Test Assertion Group");
        integrationTestHelper.setAssertionTestConfigProperty(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, "20140731");

        // Perform first time release
        integrationTestHelper.setFirstTimeRelease(true);
        integrationTestHelper.setCreateLegacyIds(true);
        final String effectiveTime1 = "20140131";
        integrationTestHelper.setEffectiveTime(effectiveTime1);
        integrationTestHelper.setReadmeHeader("This is the readme for the first release © 2002-{readmeEndDate}.\\nTable of contents:\\n");
        integrationTestHelper.setReadmeEndDate("2014");
        loadDeltaFilesToInputDirectory(effectiveTime1, false);
        final String buildURL1 = integrationTestHelper.createBuild();

        int cancelBuildStatus = integrationTestHelper.cancelBuild(buildURL1);

        //Build is not triggered so result should be failed
        Assert.assertNotEquals(200, cancelBuildStatus);

        final Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                int cancelBuildStatus1 = integrationTestHelper.cancelBuild(buildURL1);
                Assert.assertEquals(200, cancelBuildStatus1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        integrationTestHelper.triggerBuildAndGotCancelled(buildURL1);
    }


    private void loadDeltaFilesToInputDirectory(final String releaseDate, boolean isBeta) throws Exception {
        if (isBeta) {
            integrationTestHelper.uploadManifest("core_manifest_" + "beta_" + releaseDate+".xml", getClass());
        } else {
            integrationTestHelper.uploadManifest("core_manifest_" + releaseDate+".xml", getClass());
        }
        integrationTestHelper.uploadDeltaInputFile("rel2_Concept_Delta_INT_" + releaseDate + ".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_Description_Delta-en_INT_"+releaseDate +".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_TextDefinition_Delta-en_INT_"+releaseDate +".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_StatedRelationship_Delta_INT_"+releaseDate +".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_cRefset_LanguageDelta-en_INT_" + releaseDate +".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_sRefset_SimpleMapDelta_INT_" + releaseDate +".txt", getClass());
        integrationTestHelper.uploadDeltaInputFile("rel2_Relationship_Delta_INT_" + releaseDate +".txt", getClass());
    }


}
