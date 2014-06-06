package org.ihtsdo.buildcloud.service.execution;

import java.util.UUID;

/** Replace id in RefSet files with UUID generated.
 * @author mchu
 *
 */
public class UUIDTransformation implements LineTransformation {


	@Override
	public String[] transformLine(String[] columnValues) {
		columnValues[0] = UUID.randomUUID().toString();
		return columnValues;
	}

}
