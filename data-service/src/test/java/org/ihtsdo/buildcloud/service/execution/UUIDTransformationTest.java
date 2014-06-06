package org.ihtsdo.buildcloud.service.execution;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UUIDTransformationTest {
	
	private static final String EMPTY_SPACE = "";
	private static final int UUID_LENGTH = 36;
	private UUIDTransformation uuidTransformation;
	private File refSetFile;

	@Before
	public void setup() throws URISyntaxException, IOException {
		uuidTransformation = new UUIDTransformation();
		File origFile = new File(getClass().getResource("refSet-without-uuid.txt").toURI());
		Path tempFile = Files.createTempFile(getClass().getName(), origFile.getName());
		Files.copy(origFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
		refSetFile = tempFile.toFile();
	}

	@Test
	public void testReplaceSingleColumnValue() throws IOException {

		List<String> lines = Files.readAllLines(refSetFile.toPath(), StreamingFileTransformation.UTF_8);
		assertEquals(5, lines.size());
		assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId", lines.get(0));
		for( int i=1;i<lines.size();i++) {
			String[] before = lines.get(i).split("\t");
			assertEquals(EMPTY_SPACE, before[0] );
			String[] after = uuidTransformation.transformLine(before);
			assertNotEquals(EMPTY_SPACE, after[0]);
			assertEquals( UUID_LENGTH, after[0].length() );
			
		}
	}

}
