package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ReleaseFileExporter {

	public void exportFull(Connection connection, TableSchema schema, OutputStream fullOutputStream) throws SQLException, IOException {

		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + schema.getName() + " " +
				"order by id, effectiveTime"
		);

		ResultSet resultSet = preparedStatement.executeQuery();

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream))) {

			// Declare a few objects to reuse over and over.
			final StringBuilder builder = new StringBuilder();
			final List<TableSchema.Field> fields = schema.getFields();
			int fieldIndex = 1;
			String value;

			// Build header
			boolean firstField = true;
			for (TableSchema.Field field : fields) {
				if (firstField) {
					firstField = false;
				} else {
					builder.append(RF2Constants.COLUMN_SEPARATOR);
				}
				builder.append(field.getName());
			}
			builder.append(RF2Constants.LINE_ENDING);

			// Write header
			fullWriter.write(builder.toString());
			builder.setLength(0);


			// Iterate through data
			while (resultSet.next()) {
				// Assemble line for output
				fieldIndex = 1;
				for (TableSchema.Field field : fields) {
					if (field.getType() == DataType.TIME) {
						java.sql.Date date = resultSet.getDate(fieldIndex);
						value = RF2Constants.DATE_FORMAT.format(date.getTime());
					} else if (field.getType() == DataType.BOOLEAN) {
						value = resultSet.getBoolean(fieldIndex) ? "1" : "0";
					} else {
						value = resultSet.getString(fieldIndex);
					}
					if (fieldIndex > 1) {
						builder.append(RF2Constants.COLUMN_SEPARATOR);
					}
					builder.append(value);
					fieldIndex++;
				}
				builder.append(RF2Constants.LINE_ENDING);

				// Write to Full file
				fullWriter.append(builder.toString());

				builder.setLength(0); // Reset builder, reuse is cheapest.
			}
		}

	}

}
