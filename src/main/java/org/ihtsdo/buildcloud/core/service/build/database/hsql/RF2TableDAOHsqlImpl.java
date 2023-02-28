package org.ihtsdo.buildcloud.core.service.build.database.hsql;

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

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableExportDAO;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableResults;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class RF2TableDAOHsqlImpl implements RF2TableExportDAO {

	private final Connection connection;
	private final H2DataTypeConverter dataTypeConverter;
	private final SchemaFactory schemaFactory;

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableDAOHsqlImpl.class);

	public RF2TableDAOHsqlImpl(String uniqueId) throws SQLException, ClassNotFoundException {
		this.connection = new DatabaseManager().createConnection(uniqueId);
		dataTypeConverter = new H2DataTypeConverter();
		schemaFactory = new SchemaFactory();
	}

	// Testing Only
	RF2TableDAOHsqlImpl() {
		schemaFactory = null;
		dataTypeConverter = null;
		connection = null;
	}

	@Override
	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {

			// Product Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
			}
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

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws IOException, SQLException, ParseException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line

			PreparedStatement insertStatement = getInsertStatement(tableSchema, tableSchema.getTableName());
			insertData(reader, tableSchema, insertStatement);
		}
	}

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream, String previousEffectiveTime) {
		throw new UnsupportedOperationException(("Method is not supported!"));
	}

	@Override
	public RF2TableResults selectNone(TableSchema tableSchema) throws SQLException {
		String idFieldName = getIdFieldName(tableSchema);
		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + tableSchema.getTableName() + " " +
						"order by " + idFieldName + ", effectiveTime " +
						"limit 0",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY
		);

		return new RF2TableResultsHsqlImpl(preparedStatement.executeQuery(), tableSchema);
	}

	@Override
	public RF2TableResultsHsqlImpl selectAllOrdered(TableSchema tableSchema) throws SQLException {
		String idFieldName = getIdFieldName(tableSchema);
		// @formatter:off
		PreparedStatement preparedStatement = connection.prepareStatement(
				"select * from " + tableSchema.getTableName() +
						" order by " +
						idFieldName + ", effectiveTime",
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY
		);
		// @formatter:on

		// Negative values are not allowed, would need to extend JDBCStatement
		// preparedStatement.setFetchSize(Integer.MIN_VALUE); // Apparently stops the db driver caching the results
		// see http://stackoverflow.com/questions/14010595/resultset-type-forward-only-in-java#comment38287217_14010595
		return new RF2TableResultsHsqlImpl(preparedStatement.executeQuery(), tableSchema);
	}

	@Override
	public RF2TableResults selectWithEffectiveDateOrdered(TableSchema table, String effectiveDate) {
		throw new UnsupportedOperationException("This method is not yet implemented in this class (" + getClass().getName() + ")");
	}

	@Override
	public void closeConnection() throws SQLException {
		connection.close();
	}

	@Override
	public void discardAlreadyPublishedDeltaStates(InputStream previousSnapshotFileStream, String currentFullFileName, String effectiveTime) throws IOException {
		previousSnapshotFileStream.close();
		throw new UnsupportedOperationException("This method is not yet implemented in this class (" + getClass().getName() + ")");
	}

	@Override
	public void reconcileRefsetMemberIds(InputStream previousSnapshotFileStream, String currentSnapshotFileName, String effectiveTime) throws IOException {
		previousSnapshotFileStream.close();
		throw new UnsupportedOperationException("This method is not yet implemented in this class (" + getClass().getName() + ")");
	}

	private void createTable(TableSchema tableSchema) throws SQLException {
		StringBuilder producter = new StringBuilder()
				.append("create table ")
				.append(tableSchema.getTableName())
				.append(" (");

		boolean firstField = true;
		for (Field field : tableSchema.getFields()) {

			if (firstField) {
				firstField = false;
			} else {
				producter.append(", ");
			}

			DataType type = field.getType();
			String typeString = dataTypeConverter.convert(type);

			producter.append(field.getName())
					.append(" ")
					.append(typeString)
					.append(" ");
		}

		producter.append(")");
		connection.createStatement().execute(producter.toString());
	}

	private PreparedStatement getInsertStatement(TableSchema schema, String tableName) throws SQLException {
		StringBuilder producter = new StringBuilder()
				.append("insert into ")
				.append(tableName)
				.append(" (");

		boolean firstField = true;
		for (Field field : schema.getFields()) {
			if (firstField) {
				firstField = false;
			} else {
				producter.append(", ");
			}
			producter.append(field.getName());
		}
		producter.append(")");

		producter.append(" values (");
		firstField = true;
		for (Field field : schema.getFields()) {
			if (firstField) {
				firstField = false;
			} else {
				producter.append(", ");
			}
			producter.append("?");
		}
		producter.append(")");

		return connection.prepareStatement(producter.toString());
	}

	void insertData(BufferedReader reader, TableSchema tableSchema, PreparedStatement insertStatement) throws IOException, SQLException,
			ParseException {
		List<Field> fields = tableSchema.getFields();
		int fieldCount = fields.size();

		int executeBatchSize = 10000;
		String line;
		long lineNumber = 1;
		String[] lineValues;
		int valuesCount;
		Object value;
		while ((line = reader.readLine()) != null) {
			// Negative split limit means blank values are included at end of line
			lineValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
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

	private String getIdFieldName(TableSchema tableSchema) {
		return tableSchema.getFields().get(0).getName();
	}

	protected Connection getConnection() {
		return connection;
	}

	@Override
	public void resolveEmptyValueId(InputStream previousFileStream, String  effectiveTime) {
		throw new UnsupportedOperationException("This method is not supported yet for current implementation (" + getClass().getName() + ")");
	}

}
