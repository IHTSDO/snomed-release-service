package org.ihtsdo.buildcloud.core.service.build.transform;
import org.ihtsdo.buildcloud.core.entity.BuildReport;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.transform.RandomUUIDGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.ReplaceValueLineTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.StreamingFileTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.UUIDTransformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class StreamingFileTransformationTest {

	private StreamingFileTransformation fileTransformation;
	private File rf2File;
	private File tempOutputFile;
	private BuildReport report;

	@Before
	public void setup() throws URISyntaxException, IOException {
		fileTransformation = new StreamingFileTransformation(100);

		File origRf2File = new File(getClass().getResource("rf2-in.txt").toURI());
		Path tempFile = Files.createTempFile(getClass().getName(), origRf2File.getName());
		Files.copy(origRf2File.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
		rf2File = tempFile.toFile();

		tempOutputFile = Files.createTempFile(getClass().getName(), origRf2File.getName() + "output").toFile();
		report = BuildReport.getDummyReport();
	}

	@Test
	public void testStreamsClosed() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "03062014"));
		FileInputStream inputStream = new FileInputStream(rf2File);
		FileOutputStream outputStream = new FileOutputStream(tempOutputFile);

		fileTransformation.transformFile(inputStream, outputStream, rf2File.getName(), report);

		try {
			inputStream.available();
			Assert.fail("The available method should throw an exception because the stream should be closed.");
		} catch (IOException e) {
			Assert.assertEquals("Stream Closed", e.getMessage());
		}

		try {
			outputStream.write(0);
			Assert.fail("The write method should throw an exception because the stream should be closed.");
		} catch (IOException e) {
			Assert.assertEquals("Stream Closed", e.getMessage());
		}

	}

	@Test
	public void testReplaceSingleColumnValue() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "03062014"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0));
		Assert.assertEquals("First line with new value", "\t03062014\t1\t900000000000207008\t450990004\t293495006", linesAfter.get(1));
		Assert.assertEquals("Last line with new value", "\t03062014\t1\t900000000000207008\t450990004\t293104008", linesAfter.get(4));
	}

	@Test
	public void testReplaceManyColumnValues() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "03062014"));
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(2, "0"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0));
		Assert.assertEquals("First line with new value", "\t03062014\t0\t900000000000207008\t450990004\t293495006", linesAfter.get(1));
		Assert.assertEquals("Last line with new value", "\t03062014\t0\t900000000000207008\t450990004\t293104008", linesAfter.get(4));
	}
	
	@Test
	public void testReplaceUUID() throws Exception {
		fileTransformation.addTransformation(new UUIDTransformation(0, new RandomUUIDGenerator()));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Last line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0));
		for( int i =1; i < linesAfter.size() ; i++ ){
			Assert.assertNotEquals("UUID should be changed", linesBefore.get(i), linesAfter.get(i));
		}
	}
	
	@Test
	public void testReplaceEffectiveDateWhenValueIsAbsent() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "20140731", true));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesBefore.size());
		Assert.assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0));
		Assert.assertEquals("First line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1));
		Assert.assertEquals("Second line as expected", "\t\t1\t900000000000207008\t450990004\t293507007", linesBefore.get(2));
		Assert.assertEquals("Last line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4));

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		Assert.assertEquals(5, linesAfter.size());
		Assert.assertEquals("Header unchanged", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0));
		Assert.assertEquals("First line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293495006", linesAfter.get(1));
		Assert.assertEquals("Second line as expected", "\t20140731\t1\t900000000000207008\t450990004\t293507007", linesAfter.get(2));
		Assert.assertEquals("Last line as expected", "\t20140131\t1\t900000000000207008\t450990004\t293104008", linesAfter.get(4));
	}

}
