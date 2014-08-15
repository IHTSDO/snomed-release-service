package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

public class RF2TableDAOTreeMapImpl implements RF2TableDAO {

	public static final String FORMAT = "%s" + RF2Constants.COLUMN_SEPARATOR + "%s";
	private final SchemaFactory schemaFactory;
	private TableSchema tableSchema;
	private DataType idType;
	private Map<Key, String> table;

	public RF2TableDAOTreeMapImpl() {
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
	}

	@Override
	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			// Build Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = getHeader(rf2FilePath, reader);
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			idType = tableSchema.getFields().get(0).getType();
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader);

			return tableSchema;
		}

	}

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream) throws IOException, SQLException, ParseException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader);
		}
	}

	@Override
	public RF2TableResults selectAllOrdered(TableSchema tableSchema) throws SQLException {
		return new RF2TableResultsMapImpl(table);
	}

	@Override
	public RF2TableResults selectNone(TableSchema tableSchema) throws SQLException {
		return new RF2TableResultsMapImpl(new TreeMap<Key, String>());
	}

	@Override
	public void closeConnection() throws SQLException {
		table = null;
	}

	@Override
	public void discardAlreadyPublishedDeltaStates(InputStream previousFullFileStream, String currentFullFileName, String effectiveTime) throws IOException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousFullFileStream, RF2Constants.UTF_8))) {
			getHeader(currentFullFileName, reader);

			String line;
			String value;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
				Key key = getKey(parts[0], effectiveTime);
				if ((value = table.get(key)) != null) {
					if (value.equals(parts[2])) {
						// Fields after second column
						table.remove(key);
					}
				}
			}
		}
	}

	public Map<Key, String> getTable() {
		return table;
	}

	private String getHeader(String rf2FilePath, BufferedReader reader) throws IOException, DatabasePopulatorException {
		String headerLine = reader.readLine();
		if (headerLine == null) throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
		return headerLine;
	}

	private void insertData(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			table.put(getKey(parts[0], parts[1]), parts[2]);
		}
	}

	private Key getKey(String part0, String part1) {
		Key key;
		if (idType == DataType.SCTID) {
			key = new SCTIDKey(part0, part1);
		} else {
			key = new UUIDKey(part0, part1);
		}
		return key;
	}

}
