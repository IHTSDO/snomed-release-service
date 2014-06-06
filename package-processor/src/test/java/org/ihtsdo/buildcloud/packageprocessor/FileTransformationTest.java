package org.ihtsdo.buildcloud.packageprocessor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FileTransformationTest {

	private FileTransformation fileTransformation;
	private File rf2File;

	@Before
	public void setup() throws URISyntaxException, IOException {
		fileTransformation = new FileTransformation();
		File origRf2File = new File(getClass().getResource("rf2-in.txt").toURI());
		Path tempFile = Files.createTempFile(getClass().getName(), origRf2File.getName());
		Files.copy(origRf2File.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
		rf2File = tempFile.toFile();
	}

	@Test
	public void testReplaceSingleColumnValue() throws IOException {
		fileTransformation.addLineTransformation(new ReplaceValueLineTransformation(1, "03062014"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "3570b46b-b581-4655-ba2c-9a677a2e880c\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "c8e26c3c-5f19-41e7-b74b-2ebb889e9e41\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));


		fileTransformation.transformFile(rf2File);

		List<String> linesAfter = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesAfter.get(0));
		Assert.assertEquals("First line with new value", "3570b46b-b581-4655-ba2c-9a677a2e880c\t03062014\t1\t900000000000207008\t450990004\t293495006", linesAfter.get(1));
		Assert.assertEquals("Last line with new value", "c8e26c3c-5f19-41e7-b74b-2ebb889e9e41\t03062014\t1\t900000000000207008\t450990004\t293104008", linesAfter.get(4));
	}

	@Test
	public void testReplaceManyColumnValues() throws IOException {
		fileTransformation.addLineTransformation(new ReplaceValueLineTransformation(1, "03062014"));
		fileTransformation.addLineTransformation(new ReplaceValueLineTransformation(2, "0"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "3570b46b-b581-4655-ba2c-9a677a2e880c\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "c8e26c3c-5f19-41e7-b74b-2ebb889e9e41\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));


		fileTransformation.transformFile(rf2File);

		List<String> linesAfter = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesAfter.get(0));
		Assert.assertEquals("First line with new value", "3570b46b-b581-4655-ba2c-9a677a2e880c\t03062014\t0\t900000000000207008\t450990004\t293495006", linesAfter.get(1));
		Assert.assertEquals("Last line with new value", "c8e26c3c-5f19-41e7-b74b-2ebb889e9e41\t03062014\t0\t900000000000207008\t450990004\t293104008", linesAfter.get(4));
	}
	
	@Test
	public void testReplaceUUID() throws IOException {
		fileTransformation.addLineTransformation(new UUIDTransformation() );

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "3570b46b-b581-4655-ba2c-9a677a2e880c\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "c8e26c3c-5f19-41e7-b74b-2ebb889e9e41\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));


		fileTransformation.transformFile(rf2File);

		List<String> linesAfter = Files.readAllLines(rf2File.toPath(), FileTransformation.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", linesAfter.get(0));
		for( int i =1; i < linesAfter.size() ; i++ ){
			Assert.assertNotEquals("UUID is changed", linesBefore.get(i), linesAfter.get(i));
		}
	}

}
