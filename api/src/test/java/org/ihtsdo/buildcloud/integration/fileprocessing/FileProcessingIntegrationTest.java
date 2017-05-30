package org.ihtsdo.buildcloud.integration.fileprocessing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.controller.helper.IntegrationTestHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huyle
 * Date: 5/30/2017
 * Time: 3:10 PM
 */
public class FileProcessingIntegrationTest extends AbstractControllerTest{

    private IntegrationTestHelper integrationTestHelper;
    private static final String REFSET_TOOL = "reference-set-tool";
    private static final String MAPPING_TOOLS = "mapping-tools";
    private static final String MANUAL = "manual";
    private static final String MANIFEST_DIR = "manifest/";

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        integrationTestHelper = new IntegrationTestHelper(mockMvc,"FileProcessingTest");
    }


    @Test
    public void testUploadTextSourceFile() throws Exception {
        integrationTestHelper.loginAsManager();
        integrationTestHelper.createTestProductStructure();
        integrationTestHelper.uploadSourceFile("der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt", REFSET_TOOL, this.getClass());
        integrationTestHelper.uploadSourceFile("der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MAPPING_TOOLS, this.getClass());
        integrationTestHelper.uploadSourceFile("der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt", MANUAL, this.getClass());
        String fileList = integrationTestHelper.listSourceFiles().replace("\r\n","\n").replace("\r","\n");
        Assert.assertEquals("[ {\n" +
                "  \"id\" : \"manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/manual/der2_cRefset_DescriptionInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
                "}, {\n" +
                "  \"id\" : \"mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/mapping-tools/der2_cRefset_ConceptInactivationIndicatorReferenceSetDelta_INT_20170731.txt\"\n" +
                "}, {\n" +
                "  \"id\" : \"reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\",\n" +
                "  \"url\" : \"http://localhost/centers/international/products/fileprocessingtest_product/sourcefiles/reference-set-tool/der2_cRefset_AlternativeAssociationReferenceSetDelta_INT_20170731.txt\"\n" +
                "} ]", fileList);
        integrationTestHelper.deleteTxtSourceFiles();
    }


}
