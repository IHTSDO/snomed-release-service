package org.ihtsdo.buildcloud.integration.corecomponents;

import org.ihtsdo.buildcloud.rest.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.rest.controller.helper.IntegrationTestHelper;
import org.ihtsdo.buildcloud.core.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuildCancellationTestIntegration extends AbstractControllerTest{

    private static final String INTERNATIONAL_RELEASE = "SnomedCT_Release_INT_";

    private IntegrationTestHelper integrationTestHelper;

    @Override
    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        integrationTestHelper = new IntegrationTestHelper(mockMvc,"CoreComponentsTest");
    }

    @Test
    @Disabled
    public void testCancelBuild() throws Exception {
        integrationTestHelper.createTestProductStructure();

        //config assertion tests
        integrationTestHelper.setAssertionTestConfigProperty(ProductService.EXTENSION_DEPENDENCY_RELEASE, "20140731");

        // Perform first time release
        integrationTestHelper.setFirstTimeRelease(true);
        integrationTestHelper.setCreateLegacyIds(true);
        final String effectiveTime1 = "20140131";
        integrationTestHelper.setEffectiveTime(effectiveTime1);

        loadDeltaFilesToInputDirectory(effectiveTime1, false);
        final String buildURL1 = integrationTestHelper.createBuild(effectiveTime1);

        int cancelBuildStatus = integrationTestHelper.cancelBuild(buildURL1);

        //Build is not triggered so result should be failed
        assertNotEquals(200, cancelBuildStatus);

        final Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
                int cancelBuildStatus1 = integrationTestHelper.cancelBuild(buildURL1);
                assertEquals(200, cancelBuildStatus1);

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
