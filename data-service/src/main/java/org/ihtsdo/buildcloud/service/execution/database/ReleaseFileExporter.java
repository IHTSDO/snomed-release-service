package org.ihtsdo.buildcloud.service.execution.database;

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

import org.ihtsdo.buildcloud.service.execution.RF2Constants;

public class ReleaseFileExporter {

	public void exportFullAndSnapshot(Connection connection, TableSchema schema, Date targetEffectiveTime, OutputStream fullOutputStream, OutputStream snapshotOutputStream) throws SQLException, IOException {

		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + schema.getName() + " " +
				"order by id, effectiveTime"
		);

		ResultSet resultSet = preparedStatement.executeQuery();

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream));
				BufferedWriter snapshotWriter = new BufferedWriter(new OutputStreamWriter(snapshotOutputStream))) {

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

			// Write header to both files
			String header = builder.toString();
			fullWriter.write(header);
			snapshotWriter.write(header);
			builder.setLength(0);

			// Variables for snapshot resolution
			String currentId = null;
			Long currentEffectiveTime = null;
			String currentLine;
			String lastId = null;
			String lastLine = null;
			String lastWrittenId = null;
			Long lastEffectiveTime = null;
			// Iterate through data
			while (resultSet.next()) {
				// Assemble line for output
				fieldIndex = 1;
				for (TableSchema.Field field : fields) {
					if (field.getType() == DataType.TIME) {
						java.sql.Date date = resultSet.getDate(fieldIndex);
						value = RF2Constants.DATE_FORMAT.format(date.getTime());
						if (field.getName().equals(RF2Constants.EFFECTIVE_TIME)) {
							currentEffectiveTime = date.getTime();
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
				boolean isTargetTimePassed = currentEffectiveTime > targetEffectiveTime.getTime();
				// If moved on to a new member or passed target effectiveTime, write last line to Snapshot file
				if (lastId != null && (!currentId.equals(lastId) || isTargetTimePassed ) && !lastId.equals(lastWrittenId)) {
					// Write Snapshot file when effective time has not passed target time.
				    	if ( !(lastEffectiveTime > targetEffectiveTime.getTime())){
				    	    snapshotWriter.append(lastLine);
				    	    lastWrittenId = lastId;
				    	}
				}
				// Record last variables
				lastId = currentId;
				lastLine = currentLine;
				lastEffectiveTime = currentEffectiveTime;
				builder.setLength(0); // Reset builder, reuse is cheapest.
			}
		}

	}

}
