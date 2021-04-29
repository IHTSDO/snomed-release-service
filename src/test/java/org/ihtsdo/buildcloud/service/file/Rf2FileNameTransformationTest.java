package org.ihtsdo.buildcloud.service.file;

import org.junit.Assert;
import org.junit.Test;

public class Rf2FileNameTransformationTest {

	@Test
	public void test() {
		String test = "der2_ciRefset_DescriptionTypeFull_INT_20140131.txt";
		String expected = "der2_ciRefset_DescriptionTypeFull_INT";
		String actual = new Rf2FileNameTransformation().transformFilename(test);
		Assert.assertEquals(expected,actual);
	}

}
