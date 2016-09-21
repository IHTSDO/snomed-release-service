package org.ihtsdo.buildcloud.service.build.transform;

/**
 * Replace a field value when another field contains null or empty value.
 *
 */
public class ReplaceInferredRelationshipModuleIdTransformation implements LineTransformation {
	private static final String NULL = "null";
	private int checkColumn;
	private int column;
	private String value;

	public ReplaceInferredRelationshipModuleIdTransformation(int checkColumn, int column, String value) {
		this.checkColumn = checkColumn;
		this.column = column;
		this.value = value;
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		if (columnValues != null && columnValues.length > column && columnValues.length > checkColumn) {
			if (columnValues[checkColumn] == null || columnValues[checkColumn].isEmpty() || NULL.equals(columnValues[checkColumn])) {
				columnValues[column] = value;
			} 
		}
	}

	@Override
	public int getColumnIndex() {
		return column;
	}

}
