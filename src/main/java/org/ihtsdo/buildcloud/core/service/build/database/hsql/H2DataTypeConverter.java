package org.ihtsdo.buildcloud.core.service.build.database.hsql;

import org.ihtsdo.snomed.util.rf2.schema.DataType;

import java.sql.SQLException;

public class H2DataTypeConverter {

	/**
	 * @param type
	 * @return a <code>String</code> containing the H2 data type.
	 */
	public String convert(DataType type) throws SQLException {
		String h2Type = switch (type) {
            case SCTID -> "BIGINT";
            case UUID -> "UUID";
            case BOOLEAN -> "BOOLEAN";
            case TIME -> "TIMESTAMP";
            case INTEGER -> "INTEGER";
            case STRING -> "VARCHAR";
            default -> throw new SQLException("DataType missing from " + getClass() + " : " + type);
        };
        return h2Type;
	}

}
