package org.ihtsdo.buildcloud.service.execution.transform;

public class ReplaceValueLineTransformation implements LineTransformation {

	public int column;
	public String value;
	private final boolean replaceWhenAbsentOnly;

	public ReplaceValueLineTransformation(int column, String value, boolean replaceWhenAbsentOnly) {
		this.column = column;
		this.value = value;
		this.replaceWhenAbsentOnly = replaceWhenAbsentOnly;
	}


	public ReplaceValueLineTransformation(int column, String value) {
		this(column, value, false);
	}

	@Override
	public void transformLine(String[] columnValues) {
		if (columnValues != null && columnValues.length > column) {
			if (!replaceWhenAbsentOnly) {
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
