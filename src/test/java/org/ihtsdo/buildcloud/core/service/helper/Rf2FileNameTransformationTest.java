package org.ihtsdo.buildcloud.core.service.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Rf2FileNameTransformationTest {

	@Test
	public void test() {
		String test = "der2_ciRefset_DescriptionTypeFull_INT_20140131.txt";
		String expected = "der2_ciRefset_DescriptionTypeFull_INT";
		String actual = new Rf2FileNameTransformation().transformFilename(test);
		assertEquals(expected, actual);
	}

}
