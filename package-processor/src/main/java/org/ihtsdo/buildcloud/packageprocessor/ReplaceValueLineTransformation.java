package org.ihtsdo.buildcloud.packageprocessor;

public class ReplaceValueLineTransformation implements LineTransformation {

	public int column;
	public String value;

	public ReplaceValueLineTransformation(int column, String value) {
		this.column = column;
		this.value = value;
	}

	@Override
	public String[] transformLine(String[] columnValues) {
		columnValues[column] = value;
		return columnValues;
	}

}
