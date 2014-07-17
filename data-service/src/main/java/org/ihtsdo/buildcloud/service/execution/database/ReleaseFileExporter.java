package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ReleaseFileExporter {

	public void exportFullAndSnapshot(Connection connection, TableSchema schema, Date targetEffectiveTime, OutputStream fullOutputStream, OutputStream snapshotOutputStream) throws SQLException, IOException {

		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + schema.getTableName() + " " +
				"order by id, effectiveTime"
		);

		ResultSet resultSet = preparedStatement.executeQuery();

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream));
				BufferedWriter snapshotWriter = new BufferedWriter(new OutputStreamWriter(snapshotOutputStream))) {

			// Declare a few objects to reuse over and over.
			final StringBuilder builder = new StringBuilder();
			final List<Field> fields = schema.getFields();
			int fieldIndex = 1;
			String value;

			// Build header
			boolean firstField = true;
			for (Field field : fields) {
				if (firstField) {
					firstField = false;
				} else {
					builder.append(RF2Constants.COLUMN_SEPARATOR);
				}
				builder.append(field.getName());
			}
			builder.append(RF2Constants.LINE_ENDING);

			// Write header to both files
			String header = builder.toString();
			fullWriter.write(header);
			snapshotWriter.write(header);
			builder.setLength(0);

			// Variables for snapshot resolution
			String currentLine;
			String currentId = null;
			Long currentEffectiveTimeSeconds = null;
			Long targetEffectiveTimeSeconds = targetEffectiveTime.getTime();
			String lastId = null;
			String validLine = null;
			boolean movedToNewMember;
			boolean passedTargetEffectiveTime;

			// Iterate through data
			while (resultSet.next()) {
				// Assemble line for output
				fieldIndex = 1;
				for (Field field : fields) {
					if (field.getType() == DataType.TIME) {
						java.sql.Date date = resultSet.getDate(fieldIndex);
						value = RF2Constants.DATE_FORMAT.format(date.getTime());
						if (field.getName().equals(RF2Constants.EFFECTIVE_TIME)) {
							currentEffectiveTimeSeconds = date.getTime();
						}
					} else if (field.getType() == DataType.BOOLEAN) {
						value = resultSet.getBoolean(fieldIndex) ? "1" : "0";
					} else {
						value = resultSet.getString(fieldIndex);
					}
					if (fieldIndex > 1) {
						builder.append(RF2Constants.COLUMN_SEPARATOR);
					}
					if (fieldIndex == 1) {
						currentId = value;
					}
					builder.append(value);
					fieldIndex++;
				}
				builder.append(RF2Constants.LINE_ENDING);
				
				// Write to Full file
				currentLine = builder.toString();
				fullWriter.append(currentLine);

				// If moved to new member or passed target effectiveTime write any previous valid line
				movedToNewMember = lastId != null && !lastId.equals(currentId);
				passedTargetEffectiveTime = currentEffectiveTimeSeconds > targetEffectiveTimeSeconds;
				if (movedToNewMember || passedTargetEffectiveTime) {
					if (validLine != null) {
						snapshotWriter.append(validLine);
						validLine = null;
					}
				}

				// Store valid line if effectiveTime not exceeded
				if (!passedTargetEffectiveTime) {
					validLine = currentLine;
				}

				// Record last id
				lastId = currentId;
				builder.setLength(0); // Reset builder, reuse is cheapest.
			}

			// Write out any valid line not yet written
			if (validLine != null) {
				snapshotWriter.append(validLine);
			}

		}

	}

}
