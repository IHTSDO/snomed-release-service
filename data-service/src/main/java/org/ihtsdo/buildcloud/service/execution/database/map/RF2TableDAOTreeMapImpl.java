package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

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
	private Map<Key, String> table;

	public RF2TableDAOTreeMapImpl() {
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
	}

	@Override
	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream))) {
			// Build Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = reader.readLine();
			if (headerLine == null) throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader);

			return tableSchema;
		}

	}

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream) throws IOException, SQLException, ParseException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream))) {
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

	public Map<Key, String> getTable() {
		return table;
	}

	private void insertData(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			table.put(new Key(parts[0], parts[1]), parts[2]);
		}
	}

}
