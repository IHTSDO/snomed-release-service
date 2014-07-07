package org.ihtsdo.buildcloud.service.execution.transform;


/** Replace id in RefSet files with UUID generated.
 */
public class UUIDTransformation implements LineTransformation {

	public int column;
	private final UUIDGenerator uuidGenerator;

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
