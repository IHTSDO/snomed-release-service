package org.ihtsdo.buildcloud.execution;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.maven.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

public class PackageBuilderTest {

	@Test
	public void testPackaging() throws Exception{
		File manifestFile = new File(getClass().getResource("test_manifest.xml").getFile());
		
		String manifestPath = manifestFile.getAbsolutePath();
		
		//A file for putting in our package is expected to be in the same directory as the manifest
		String sourcePath = FilenameUtils.getFullPathNoEndSeparator(manifestPath);
		String targetPath = Files.createTempDir().getAbsolutePath();
		
		if (!manifestFile.exists()) {
			throw new Exception ("Unable to locate test_manifest.xml in expected location: " + sourcePath);
		}
		
		PackageBuilder pb = new PackageBuilder(sourcePath, targetPath, "testZipFile.zip", manifestPath);
		
		//We're expecting to be missing about 89 files during testing, so suppress those warnings
		pb.warnOnMissingFiles(false);
		File zipFile = pb.process();
		
		int itemsInTargetDirectory = TestUtils.itemCount(new File(targetPath));
		Assert.assertEquals("Expecting 49 directories + 1 file = 50 items in target directory", 50, itemsInTargetDirectory);
		
		Map<String, String> zipContents = FileUtils.examineZipContents(zipFile.getName(), new FileInputStream(zipFile));
		Assert.assertEquals("Expecting 49 directories + 1 file = 50 items in zipped file", 50, zipContents.size());
		
		//And lets make sure our test file is in there.
		Assert.assertTrue(zipContents.containsValue(FilenameUtils.separatorsToSystem("/SnomedCT_Release_INT_20140131/RF2Release/Full/Refset/Metadata/der2_ciRefset_DescriptionTypeFull_INT_20140131.txt")));
	}

}
