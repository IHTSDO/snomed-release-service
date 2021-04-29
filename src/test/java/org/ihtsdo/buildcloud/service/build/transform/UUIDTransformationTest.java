package org.ihtsdo.buildcloud.service.build.transform;

import org.ihtsdo.buildcloud.service.build.RF2Constants;
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
		uuidTransformation = new UUIDTransformation(0, new RandomUUIDGenerator());
		File origFile = new File(getClass().getResource("refSet-without-uuid.txt").toURI());
		Path tempFile = Files.createTempFile(getClass().getName(), origFile.getName());
		Files.copy(origFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
		refSetFile = tempFile.toFile();
	}

	@Test
	public void testReplaceSingleColumnValue() throws IOException {
		List<String> lines = Files.readAllLines(refSetFile.toPath(), RF2Constants.UTF_8);
		assertEquals(5, lines.size());
		assertEquals("Header as expected", "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId", lines.get(0));
		for (int i = 1; i < lines.size(); i++) {
			String[] values = lines.get(i).split("\t");
			assertEquals(EMPTY_SPACE, values[0]);

			uuidTransformation.transformLine(values);

			assertNotEquals(EMPTY_SPACE, values[0]);
			assertEquals(UUID_LENGTH, values[0].length());
		}
	}

	@Test
	public void testKeepExistingID() {
		String existingValue = "c651597b-4956-4a7d-8f7a-5fba70609bea";
		String[] values = {existingValue};

		assertEquals(existingValue, values[0]);
		uuidTransformation.transformLine(values);
		assertEquals(existingValue, values[0]);
	}

}
