package org.ihtsdo.buildcloud.service.execution.transform;

import java.util.UUID;

/** Replace id in RefSet files with UUID generated.
 */
public class UUIDTransformation implements LineTransformation {

	public int column;

	public UUIDTransformation(int column) {
		this.column = column;
	}

	@Override
	public void transformLine(String[] columnValues) {
		if (columnValues[column].isEmpty()) {
			columnValues[column] = UUID.randomUUID().toString();
		}
	}

}
