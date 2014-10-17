package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Rf2FileWriter {

	public void exportDelta(RF2TableResults tableResults, TableSchema tableSchema, OutputStream deltaOutputStream) throws SQLException, IOException {
		try (BufferedWriter deltaWriter = new BufferedWriter(new OutputStreamWriter(deltaOutputStream, RF2Constants.UTF_8))) {
			List<Field> fields = tableSchema.getFields();

			// Write header
			String header = buildHeader(fields);
			deltaWriter.write(header);
			deltaWriter.append(RF2Constants.LINE_ENDING);

			String line;
			while ((line = tableResults.nextLine()) != null) {
				deltaWriter.append(line);
				deltaWriter.append(RF2Constants.LINE_ENDING);
			}
		}
	}

	public void exportFullAndSnapshot(RF2TableResults tableResults, TableSchema schema, Date targetEffectiveTime, OutputStream fullOutputStream, OutputStream snapshotOutputStream) throws SQLException, IOException {

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream, RF2Constants.UTF_8));
			 BufferedWriter snapshotWriter = new BufferedWriter(new OutputStreamWriter(snapshotOutputStream, RF2Constants.UTF_8))) {

			// Declare a few objects to reuse over and over.
			final StringBuilder builder = new StringBuilder();
			final List<Field> fields = schema.getFields();

			// Build header
			String header = buildHeader(fields);
			fullWriter.write(header);
			fullWriter.append(RF2Constants.LINE_ENDING);
			snapshotWriter.write(header);
			snapshotWriter.append(RF2Constants.LINE_ENDING);

			// Variables for snapshot resolution
			String currentLine;
			String currentId;
			Integer currentEffectiveTimeInt;
			Integer targetEffectiveTimeInt = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(targetEffectiveTime));
			String lastId = null;
			String validLine = null;
			boolean movedToNewMember;
			boolean passedTargetEffectiveTime;

			// Iterate through data
			while ((currentLine = tableResults.nextLine()) != null) {
				// Write to Full file
				fullWriter.append(currentLine);
				fullWriter.append(RF2Constants.LINE_ENDING);

				// Parse out id and effectiveTime
				String[] lineParts = currentLine.split(RF2Constants.COLUMN_SEPARATOR, 3);
				currentId = lineParts[0];
				currentEffectiveTimeInt = Integer.parseInt(lineParts[1]);

				// If moved to new member or passed target effectiveTime write any previous valid line
				movedToNewMember = lastId != null && !lastId.equals(currentId);
				passedTargetEffectiveTime = currentEffectiveTimeInt > targetEffectiveTimeInt;
				if (movedToNewMember || passedTargetEffectiveTime) {
					if (validLine != null) {
						snapshotWriter.append(validLine);
						snapshotWriter.append(RF2Constants.LINE_ENDING);
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
				snapshotWriter.append(RF2Constants.LINE_ENDING);
			}

		}

	}

	public void exportFull(RF2TableResults results, TableSchema tableSchema, OutputStream fullOutputStream) throws IOException, SQLException {
		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream, RF2Constants.UTF_8))) {
			String header = buildHeader(tableSchema.getFields());
			fullWriter.append(header);
			fullWriter.append(RF2Constants.LINE_ENDING);

			String line;
			while ((line = results.nextLine()) != null) {
				fullWriter.append(line);
				fullWriter.append(RF2Constants.LINE_ENDING);
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
		return builder.toString();
	}
}
