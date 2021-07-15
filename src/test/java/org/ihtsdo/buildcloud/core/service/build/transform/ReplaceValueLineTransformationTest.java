package org.ihtsdo.buildcloud.core.service.build.transform;

import org.junit.Assert;
import org.junit.Test;

public class ReplaceValueLineTransformationTest {

	private ReplaceValueLineTransformation subject;

	@Test
	public void testReplaceWhenNotAbsent() {
		subject = new ReplaceValueLineTransformation(1, "20140731", true);
		String[] columnValues = {"", "20150131"};
		String[] expected = {"", "20150131"};
		subject.transformLine(columnValues);
		Assert.assertArrayEquals(expected, columnValues);
	}

	@Test
	public void testReplaceWhenValueIsNull() {
		subject = new ReplaceValueLineTransformation(1, "20140731", true);
		String[] columnValues = {"", null};
		String[] expected = {"", "20140731"};
		subject.transformLine(columnValues);
		Assert.assertArrayEquals(expected, columnValues);
	}

	@Test
	public void testReplaceWhenValueIsEmpty() {
		subject = new ReplaceValueLineTransformation(1, "20140731", true);
		String[] columnValues = {"", ""};
		String[] expected = {"", "20140731"};
		subject.transformLine(columnValues);
		Assert.assertArrayEquals(expected, columnValues);
	}

	@Test
	public void testReplaceNoMatterWhat() {
		subject = new ReplaceValueLineTransformation(1, "20140731", false);
		String[] columnValues = {"", "20150131"};
		String[] expected = {"", "20140731"};
		subject.transformLine(columnValues);
		Assert.assertArrayEquals(expected, columnValues);
	}

}
