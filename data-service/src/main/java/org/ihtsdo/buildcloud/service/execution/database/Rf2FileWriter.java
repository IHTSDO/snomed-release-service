package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class Rf2FileWriter {

	public void exportDelta(ResultSet resultSet, TableSchema tableSchema, OutputStream deltaOutputStream) throws IOException, SQLException {
		try (BufferedWriter deltaWriter = new BufferedWriter(new OutputStreamWriter(deltaOutputStream))) {
			List<Field> fields = tableSchema.getFields();

			// Write header
			String header = buildHeader(fields);
			deltaWriter.write(header);

			final StringBuilder builder = new StringBuilder();
			int fieldIndex;

			while (resultSet.next()) {
				fieldIndex = 1;
				for (Field field : fields) {
					writeField(resultSet, field, fieldIndex++, builder);
				}
				builder.append(RF2Constants.LINE_ENDING);
				deltaWriter.append(builder);
				builder.setLength(0);
			}
		}
	}

	public void exportFullAndSnapshot(ResultSet resultSet, TableSchema schema, Date targetEffectiveTime, OutputStream fullOutputStream, OutputStream snapshotOutputStream) throws SQLException, IOException {

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream));
				BufferedWriter snapshotWriter = new BufferedWriter(new OutputStreamWriter(snapshotOutputStream))) {

			// Declare a few objects to reuse over and over.
			final StringBuilder builder = new StringBuilder();
			final List<Field> fields = schema.getFields();
			int fieldIndex;
			String value;

			// Build header
			String header = buildHeader(fields);
			fullWriter.write(header);
			snapshotWriter.write(header);

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
					value = writeField(resultSet, field, fieldIndex, builder);
					if (fieldIndex == 1) {
						currentId = value;
					}
					if (field.getName().equals(RF2Constants.EFFECTIVE_TIME)) {
						currentEffectiveTimeSeconds = resultSet.getDate(fieldIndex).getTime();
					}
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

	private String buildHeader(List<Field> fields) {
		StringBuilder builder = new StringBuilder();
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
		return builder.toString();
	}

	private String writeField(ResultSet resultSet, Field field, int fieldIndex, StringBuilder builder) throws SQLException {
		String value;
		if (field.getType() == DataType.TIME) {
			value = RF2Constants.DATE_FORMAT.format(resultSet.getDate(fieldIndex).getTime());
		} else if (field.getType() == DataType.BOOLEAN) {
			value = resultSet.getBoolean(fieldIndex) ? RF2Constants.BOOLEAN_TRUE : RF2Constants.BOOLEAN_FALSE;
		} else {
			value = resultSet.getString(fieldIndex);
		}
		if (fieldIndex > 1) {
			builder.append(RF2Constants.COLUMN_SEPARATOR);
		}
		builder.append(value);
		return value;
	}

}
