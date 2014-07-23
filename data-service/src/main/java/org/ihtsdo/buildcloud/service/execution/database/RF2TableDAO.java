package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public class RF2TableDAO {

	private final Connection connection;
	private final H2DataTypeConverter dataTypeConverter;
	private final SchemaFactory schemaFactory;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableDAO.class);

	public RF2TableDAO(String uniqueId) throws DatabasePopulatorException, SQLException, ClassNotFoundException {
		this.connection = new DatabaseManager().createConnection(uniqueId);
		dataTypeConverter = new H2DataTypeConverter();
		schemaFactory = new SchemaFactory();
	}

	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream))) {

			// Build Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = reader.readLine();
			if (headerLine == null) throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
			TableSchema tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Create Table
			String tableName = tableSchema.getTableName();
			createTable(tableSchema);

			// Insert Data
			PreparedStatement insertStatement = getInsertStatement(tableSchema, tableName);
			insertData(reader, tableSchema, insertStatement);

			return tableSchema;
		}
	}

	public void appendData(TableSchema tableSchema, InputStream rf2InputStream) throws IOException, SQLException, ParseException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream))) {
			reader.readLine(); // Discard header line

			PreparedStatement insertStatement = getInsertStatement(tableSchema, tableSchema.getTableName());
			insertData(reader, tableSchema, insertStatement);
		}
	}

	private void createTable(TableSchema tableSchema) throws SQLException {
		StringBuilder builder = new StringBuilder()
				.append("create table ")
				.append(tableSchema.getTableName())
				.append(" (");

		boolean firstField = true;
		for (Field field : tableSchema.getFields()) {

			if (firstField) {
				firstField = false;
			} else {
				builder.append(", ");
			}

			DataType type = field.getType();
			String typeString = dataTypeConverter.convert(type);

			builder.append(field.getName())
					.append(" ")
					.append(typeString)
					.append(" ");
		}

		builder.append(")");
		connection.createStatement().execute(builder.toString());
	}

	private PreparedStatement getInsertStatement(TableSchema schema, String tableName) throws SQLException {
		StringBuilder builder = new StringBuilder()
				.append("insert into ")
				.append(tableName)
				.append(" (");

		boolean firstField = true;
		for (Field field : schema.getFields()) {
			if (firstField) {
				firstField = false;
			} else {
				builder.append(", ");
			}
			builder.append(field.getName());
		}
		builder.append(")");

		builder.append(" values (");
		firstField = true;
		for (Field field : schema.getFields()) {
			if (firstField) {
				firstField = false;
			} else {
				builder.append(", ");
			}
			builder.append("?");
		}
		builder.append(")");

		return connection.prepareStatement(builder.toString());
	}

	private void insertData(BufferedReader reader, TableSchema tableSchema, PreparedStatement insertStatement) throws IOException, SQLException, ParseException {
		List<Field> fields = tableSchema.getFields();
		int fieldCount = fields.size();

		int executeBatchSize = 10000;
		String line;
		long lineNumber = 1;
		String[] lineValues;
		int valuesCount;
		Object value;
		while ((line = reader.readLine()) != null) {
			lineValues = line.split(RF2Constants.COLUMN_SEPARATOR);
			valuesCount = lineValues.length;
			if (valuesCount == fieldCount) {
				for (int colIndex = 0; colIndex < valuesCount; colIndex++) {
					DataType type = fields.get(colIndex).getType();
					if (type == DataType.TIME) {
						value = RF2Constants.DATE_FORMAT.parse(lineValues[colIndex]);
					} else {
						value = lineValues[colIndex];
					}
					insertStatement.setObject(colIndex + 1, value);
				}
				insertStatement.addBatch();
			} else {
				LOGGER.error("Line number {} has wrong column count. Expected {}, got {}. Line skipped.", lineNumber, fieldCount, valuesCount);
			}
			lineNumber++;
			if (lineNumber % executeBatchSize == 0) {
				insertStatement.executeBatch();
			}
		}
		insertStatement.executeBatch();
	}

	public ResultSet selectAllOrdered(TableSchema tableSchema) throws SQLException {
		String idFieldName = getIdFieldName(tableSchema);
		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + tableSchema.getTableName() + " " +
						"order by " + idFieldName + ", effectiveTime"
		);

		return preparedStatement.executeQuery();
	}

	public ResultSet selectNone(TableSchema tableSchema) throws SQLException {
		String idFieldName = getIdFieldName(tableSchema);
		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + tableSchema.getTableName() + " " +
						"order by " + idFieldName + ", effectiveTime " +
						"limit 0"
		);

		return preparedStatement.executeQuery();
	}

	private String getIdFieldName(TableSchema tableSchema) {
		return tableSchema.getFields().get(0).getName();
	}

	public void closeConnection() throws SQLException {
		connection.close();
	}

	protected Connection getConnection() {
		return connection;
	}

}
