package org.ihtsdo.buildcloud.core.service.build.transform;
import org.ihtsdo.buildcloud.core.entity.BuildReport;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.transform.RandomUUIDGenerator;
import org.ihtsdo.buildcloud.core.service.build.transform.ReplaceValueLineTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.StreamingFileTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.UUIDTransformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StreamingFileTransformationTest {

	private StreamingFileTransformation fileTransformation;
	private File rf2File;
	private File tempOutputFile;
	private BuildReport report;

	@BeforeEach
	public void setup() throws URISyntaxException, IOException {
		fileTransformation = new StreamingFileTransformation(100);

		File origRf2File = new File(getClass().getResource("rf2-in.txt").toURI());
		Path tempFile = Files.createTempFile(getClass().getName(), origRf2File.getName());
		Files.copy(origRf2File.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
		rf2File = tempFile.toFile();

		tempOutputFile = Files.createTempFile(getClass().getName(), origRf2File.getName() + "output").toFile();
		tempOutputFile.deleteOnExit();
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
			fail("The available method should throw an exception because the stream should be closed.");
		} catch (IOException e) {
			assertEquals("Stream Closed", e.getMessage());
		}

		try {
			outputStream.write(0);
			fail("The write method should throw an exception because the stream should be closed.");
		} catch (IOException e) {
			assertEquals("Stream Closed", e.getMessage());
		}

	}

	@Test
	public void testReplaceSingleColumnValue() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "03062014"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesBefore.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0), "Header as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1), "First line as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4), "Last line as expected");

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesAfter.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0), "Header unchanged");
		assertEquals("\t03062014\t1\t900000000000207008\t450990004\t293495006", linesAfter.get(1), "First line with new value");
		assertEquals("\t03062014\t1\t900000000000207008\t450990004\t293104008", linesAfter.get(4), "Last line with new value");
	}

	@Test
	public void testReplaceManyColumnValues() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "03062014"));
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(2, "0"));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesBefore.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0), "Header as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1), "First line as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4), "Last line as expected");

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesAfter.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0), "Header unchanged");
		assertEquals("\t03062014\t0\t900000000000207008\t450990004\t293495006", linesAfter.get(1), "First line with new value");
		assertEquals("\t03062014\t0\t900000000000207008\t450990004\t293104008", linesAfter.get(4), "Last line with new value");
	}
	
	@Test
	public void testReplaceUUID() throws Exception {
		fileTransformation.addTransformation(new UUIDTransformation(0, new RandomUUIDGenerator()));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesBefore.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0), "Header as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1), "First line as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4), "Last line as expected");

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesAfter.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0), "Header unchanged");
		for( int i =1; i < linesAfter.size() ; i++ ){
			assertNotEquals(linesBefore.get(i), linesAfter.get(i), "UUID should be changed");
		}
	}
	
	@Test
	public void testReplaceEffectiveDateWhenValueIsAbsent() throws Exception {
		fileTransformation.addTransformation(new ReplaceValueLineTransformation(1, "20140731", true));

		// Assert preconditions
		List<String> linesBefore = Files.readAllLines(rf2File.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesBefore.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesBefore.get(0), "Header as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293495006", linesBefore.get(1), "First line as expected");
		assertEquals("\t\t1\t900000000000207008\t450990004\t293507007", linesBefore.get(2), "Second line as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293104008", linesBefore.get(4), "Last line as expected");

		fileTransformation.transformFile(new FileInputStream(rf2File), new FileOutputStream(tempOutputFile), rf2File.getName(), report);

		List<String> linesAfter = Files.readAllLines(tempOutputFile.toPath(), RF2Constants.UTF_8);
		assertEquals(5, linesAfter.size());
		assertEquals("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", linesAfter.get(0), "Header unchanged");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293495006", linesAfter.get(1), "First line as expected");
		assertEquals("\t20140731\t1\t900000000000207008\t450990004\t293507007", linesAfter.get(2), "Second line as expected");
		assertEquals("\t20140131\t1\t900000000000207008\t450990004\t293104008", linesAfter.get(4), "Last line as expected");
	}

}
