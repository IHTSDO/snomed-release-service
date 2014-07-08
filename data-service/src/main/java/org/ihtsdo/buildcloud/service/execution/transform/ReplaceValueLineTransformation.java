package org.ihtsdo.buildcloud.service.execution.transform;

public class ReplaceValueLineTransformation implements LineTransformation {

	public int column;
	public String value;

	public ReplaceValueLineTransformation(int column, String value) {
		this.column = column;
		this.value = value;
	}

	@Override
	public void transformLine(String[] columnValues) {
	    if(columnValues != null && columnValues.length > column){
		columnValues[column] = value;
	    }
	}

	@Override
	public int getColumnIndex() {
		return column;
	}
}
