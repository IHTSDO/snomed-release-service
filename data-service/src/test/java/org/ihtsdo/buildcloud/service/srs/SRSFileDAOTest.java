package org.ihtsdo.buildcloud.service.srs;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * User: huyle
 * Date: 5/23/2017
 * Time: 2:24 PM
 */
public class SRSFileDAOTest {

    public SRSFileDAO srsFileDAO;
    private static String TEST_FILE = "rel2_Concept_Delta_INT_20150318.txt";

    @Before
    public void setup() {
        srsFileDAO = new SRSFileDAO(null,false);
    }

    
    @Test
    public void testCreateSubsetFile() throws IOException {
        URL testFileURL = getClass().getResource(TEST_FILE);
        File testFile = new File(testFileURL.getFile());
        File targetDirectory = Files.createTempDir();
        File targetFile = new File(targetDirectory, "targetFile.txt");
        srsFileDAO.createSubsetFile(testFile, targetFile, 1, "Unpublished", false, false);
        List<String> allLines = FileUtils.readLines(targetFile, StandardCharsets.UTF_8);
        Assert.assertEquals(2, allLines.size());
    }

}
