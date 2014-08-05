package org.ihtsdo.buildcloud.service.execution.database.hsql;

import org.ihtsdo.snomed.util.rf2.schema.DataType;

public class H2DataTypeConverter {

	/**
	 * @param type
	 * @return a <code>String</code> containing the H2 data type.
	 */
	public String convert(DataType type) {
		String h2Type;
		switch (type) {
			case SCTID:
				h2Type = "BIGINT";
				break;
			case UUID:
				h2Type = "UUID";
				break;
			case BOOLEAN:
				h2Type = "BOOLEAN";
				break;
			case TIME:
				h2Type = "TIMESTAMP";
				break;
			case INTEGER:
				h2Type = "INTEGER";
				break;
			case STRING:
				h2Type = "VARCHAR";
				break;
			default:
				throw new RuntimeException("DataType missing from " + getClass() + " : " + type);
		}
		return h2Type;
	}

}
