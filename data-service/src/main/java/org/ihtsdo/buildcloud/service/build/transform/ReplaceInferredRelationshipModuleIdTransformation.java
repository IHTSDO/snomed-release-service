package org.ihtsdo.buildcloud.service.build.transform;

/**
 * Replace a field value when another field contains null or empty value.
 *
 */
public class ReplaceInferredRelationshipModuleIdTransformation implements LineTransformation {
	private int checkColumn;
	private String checkColumnValue;
	private int column;
	private String value;

	public ReplaceInferredRelationshipModuleIdTransformation(int checkColumn, String checkColumnValue, int column, String value) {
		this.checkColumn = checkColumn;
		this.checkColumnValue = checkColumnValue;
		this.column = column;
		this.value = value;
		
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues != null && columnValues.length > column && columnValues.length > checkColumn) {
			if (columnValues[checkColumn] == null || columnValues[checkColumn].isEmpty() || checkColumnValue.equals(columnValues[checkColumn])) {
				columnValues[column] = value;
			} 
		}
	}

	@Override
	public int getColumnIndex() {
		return column;
	}

}
