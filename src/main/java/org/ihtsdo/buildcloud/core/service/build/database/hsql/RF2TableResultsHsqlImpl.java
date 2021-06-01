package org.ihtsdo.buildcloud.core.service.build.database.hsql;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableResults;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RF2TableResultsHsqlImpl implements RF2TableResults {

	private final ResultSet resultSet;
	private final StringBuilder producter;
	private final TableSchema tableSchema;
	private final List<Field> fields;
	private int fieldIndex;

	public RF2TableResultsHsqlImpl(ResultSet resultSet, TableSchema tableSchema) {
		this.resultSet = resultSet;
		this.tableSchema = tableSchema;
		fields = tableSchema.getFields();
		producter = new StringBuilder();
	}

	@Override
	public String nextLine() throws SQLException {
		if (resultSet.next()) {
			producter.setLength(0);
			fieldIndex = 1;
			for (Field field : fields) {
				writeField(resultSet, field, fieldIndex++, producter);
			}
			return producter.toString();
		} else {
			return null;
		}
	}

	private String writeField(ResultSet resultSet, Field field, int fieldIndex, StringBuilder producter) throws SQLException {
		String value;
		if (field.getType() == DataType.TIME) {
			value = RF2Constants.DATE_FORMAT.format(resultSet.getDate(fieldIndex).getTime());
		} else if (field.getType() == DataType.BOOLEAN) {
			value = resultSet.getBoolean(fieldIndex) ? RF2Constants.BOOLEAN_TRUE : RF2Constants.BOOLEAN_FALSE;
		} else {
			value = resultSet.getString(fieldIndex);
		}
		if (fieldIndex > 1) {
			producter.append(RF2Constants.COLUMN_SEPARATOR);
		}
		producter.append(value);
		return value;
	}

}
