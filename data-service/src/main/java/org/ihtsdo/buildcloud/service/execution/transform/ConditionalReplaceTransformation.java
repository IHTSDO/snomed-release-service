package org.ihtsdo.buildcloud.service.execution.transform;

import java.util.Set;

public class ConditionalReplaceTransformation implements LineTransformation {

	private final int columnIndexToTest;
	private final Set<String> testValueInThisSet;
	private final int columnIndexToReplace;
	private final String replacementValue;

	public ConditionalReplaceTransformation(int columnIndexToTest, Set<String> testValueInThisSet,
			int columnIndexToReplace, String replacementValue) {
		this.columnIndexToTest = columnIndexToTest;
		this.testValueInThisSet = testValueInThisSet;
		this.columnIndexToReplace = columnIndexToReplace;
		this.replacementValue = replacementValue;
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		int maxIndex = columnValues.length - 1;
		if (columnIndexToTest <= maxIndex && columnIndexToReplace <= maxIndex) {
			if (testValueInThisSet.contains(columnValues[columnIndexToTest])) {
				columnValues[columnIndexToReplace] = replacementValue;
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return columnIndexToReplace;
	}

}
