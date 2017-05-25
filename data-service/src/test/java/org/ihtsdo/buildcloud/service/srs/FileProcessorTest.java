package org.ihtsdo.buildcloud.service.srs;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * User: huyle
 * Date: 5/24/2017
 * Time: 9:36 PM
 */
public class FileProcessorTest {

    private static String TEST_ARCHIVE = "test_archive_20150318.zip";

    private File testArchive = null;
    private FileProcessor fileProcessor = null;


    @Before
    public void setUp() throws Exception {
        URL testArchiveURL = getClass().getResource(TEST_ARCHIVE);
        testArchive = new File(testArchiveURL.getFile());
        if (!testArchive.exists()) {
            throw new Exception("Unable to load test resource: " + TEST_ARCHIVE);
        }
    }

 

}
