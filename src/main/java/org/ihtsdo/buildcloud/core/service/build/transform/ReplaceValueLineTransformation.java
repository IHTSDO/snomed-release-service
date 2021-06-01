package org.ihtsdo.buildcloud.core.service.build.transform;

public class ReplaceValueLineTransformation implements LineTransformation {

	public int column;
	public String value;
	private final boolean keepExistingValueIfPresent;

	public ReplaceValueLineTransformation(int column, String value, boolean keepExistingValueIfPresent) {
		this.column = column;
		this.value = value;
		this.keepExistingValueIfPresent = keepExistingValueIfPresent;
	}


	public ReplaceValueLineTransformation(int column, String value) {
		this(column, value, false);
	}

	@Override
	public void transformLine(String[] columnValues) {
		if (columnValues != null && columnValues.length > column) {
			if (!keepExistingValueIfPresent) {
				columnValues[column] = value;
			} else {
				if (columnValues[column] == null || columnValues[column].isEmpty()) {
					columnValues[column] = value;
				}
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return column;
	}
}
