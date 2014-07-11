package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InputFilesExistenceCheckTest extends PreconditionCheckTest{
    @Autowired
    private InputFilesExistenceCheck inputFilesCheck;

    @Override
    @Before
    public void setup() {
	super.setup();
	manager = new PreconditionManager().preconditionChecks(inputFilesCheck);
    }

    @Test
    public void checkInputFilesNotExisting() throws Exception {
	loadManifest("valid_manifest.xml");
	State actualResult = runPreConditionCheck(InputFilesExistenceCheck.class);
	Assert.assertEquals( State.FATAL, actualResult);
    }

    @Test
    public void checkInputFilesNotMatchingManifest() throws Exception {
	addEmptyFileToInputDirectory("der2_Refset_SimpleDelta_INT_20140831.txt");
	loadManifest("valid_manifest.xml");
	State actualResult = runPreConditionCheck(InputFilesExistenceCheck.class);
	Assert.assertEquals( State.FATAL, actualResult);
    }

    @Test
    public void checkInputFilesMatchingManifest() throws Exception {
	addEmptyFileToInputDirectory("rel2_Refset_SimpleDelta_INT_20140831.txt");
	loadManifest("valid_manifest.xml");
	State actualResult = runPreConditionCheck(InputFilesExistenceCheck.class);
	Assert.assertEquals( State.PASS, actualResult);
    }
    
    @Test
    public void checkInputFilesMathingCoreRefSetManifest() throws Exception {
	addEmptyFileToInputDirectory("rel2_cRefset_LanguageDelta-en_INT_20140731.txt");
	addEmptyFileToInputDirectory("rel2_Concept_Delta_INT_20140731.txt");
	addEmptyFileToInputDirectory("rel2_Description_Delta-en_INT_20140731.txt");
	addEmptyFileToInputDirectory("rel2_StatedRelationship_Delta_INT_20140731.txt");
	addEmptyFileToInputDirectory("rel2_TextDefinition_Delta-en_INT_20140731.txt");
	addEmptyFileToInputDirectory("zres2_icRefset_OrderedTypeFull_INT_20140731.txt");
	addEmptyFileToInputDirectory("Readme_US_EN_20140731.txt");
	addEmptyFileToInputDirectory("doc_SnomedCTReleaseNotes_Current-en-US_INT_20140731.pdf");
	loadManifest("valid_core_manifest.xml");
	State actualResult = runPreConditionCheck(InputFilesExistenceCheck.class);
	Assert.assertEquals( State.PASS, actualResult);
    }
    
    @After
    public void tearDown() throws ResourceNotFoundException{
	deleteFilesFromInputFileByPattern("*.txt");
	deleteFilesFromInputFileByPattern("*.pdf");
    }


}
