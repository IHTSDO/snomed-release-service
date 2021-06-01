package org.ihtsdo.buildcloud.core.service.build.transform;

/**
 * Replace id in RefSet files with UUID generated.
 */
public class UUIDTransformation implements LineTransformation {

	private final UUIDGenerator uuidGenerator;

	public int column;

	public UUIDTransformation(int column, UUIDGenerator uuidGenerator) {
		this.column = column;
		this.uuidGenerator = uuidGenerator;
	}

	@Override
	public void transformLine(String[] columnValues) {
		if (columnValues != null && columnValues.length > column && columnValues[column].isEmpty()) {
			columnValues[column] = uuidGenerator.uuid();
		}
	}

	@Override
	public int getColumnIndex() {
		return column;
	}

}
