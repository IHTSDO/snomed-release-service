package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.execution.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RF2TableDAOTreeMapImpl implements RF2TableDAO {

	private final SchemaFactory schemaFactory;
	private TableSchema tableSchema;
	private DataType idType;
	private Map<Key, String> table;
	private List<Key> dirtyKeys;
	private UUIDGenerator uuidGenerator;

	private static final Pattern REFSET_ID_AND_REFERENCED_COMPONENT_ID_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*\t[^\t]*)");
	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableDAOTreeMapImpl.class);

	public RF2TableDAOTreeMapImpl(UUIDGenerator uuidGenerator) {
		this.uuidGenerator = uuidGenerator;
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
	}

	@Override
	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream, boolean firstTimeRelease, boolean workbenchDataFixesRequired) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			// Build Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = getHeader(rf2FilePath, reader);
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			idType = tableSchema.getFields().get(0).getType();
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader, tableSchema, true, firstTimeRelease, workbenchDataFixesRequired);

			return tableSchema;
		}

	}

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws IOException, SQLException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader, tableSchema, false, workbenchDataFixesRequired, workbenchDataFixesRequired);
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
	public void discardAlreadyPublishedDeltaStates(InputStream previousSnapshotFileStream, String currentSnapshotFileName, String effectiveTime) throws IOException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			getHeader(currentSnapshotFileName, reader);

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

	@Override
	public void reconcileRefsetMemberIds(InputStream previousSnapshotFileStream, String currentSnapshotFileName, String effectiveTime) throws IOException, DatabasePopulatorException {
		try (BufferedReader prevSnapshotReader = new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			getHeader("previous to " + currentSnapshotFileName, prevSnapshotReader);

			String line;
			String[] parts;
			while ((line = prevSnapshotReader.readLine()) != null) {
				String compositeKey = getCompositeKey(line);
				Key matcherKey = new StringKey(compositeKey);
				String newValues = table.get(matcherKey);
				if (newValues != null) {
					parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
					replaceDirtyKey(matcherKey, parts[0], effectiveTime);
				}
			}
		}

		if (!dirtyKeys.isEmpty()) {
			LOGGER.info("{} dirty refset ids remain. Generating UUIDs for new members.", dirtyKeys.size());
			while (dirtyKeys.iterator().hasNext()) {
				Key remainingDirtyKey = dirtyKeys.iterator().next();
				String newKey = uuidGenerator.uuid();
				replaceDirtyKey(remainingDirtyKey, newKey, effectiveTime);
			}
		} else {
			LOGGER.info("No dirty refset ids remain. No new members.", dirtyKeys.size());
		}
	}

	private void replaceDirtyKey(Key existingDirtyKey, String newKeyUUID, String effectiveTime) throws DatabasePopulatorException {
		String existingData = table.remove(existingDirtyKey);
		if (existingData != null) {
			table.put(new UUIDKey(newKeyUUID, effectiveTime), existingData);
			if (!dirtyKeys.remove(existingDirtyKey)) {
				throw new DatabasePopulatorException("Failed to remove dirty key " + existingDirtyKey + "'");
			}
		} else {
			throw new DatabasePopulatorException("No match found when replacing dirty key '" + existingDirtyKey + "'");
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

	private void insertData(BufferedReader reader, TableSchema tableSchema, boolean deltaData, boolean firstTimeRelease, boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException {
		String line;
		String[] parts;
		Key key;
		dirtyKeys = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			if (workbenchDataFixesRequired && deltaData && tableSchema.getComponentType() == ComponentType.REFSET) {
				if (firstTimeRelease) {
					// Assign new uuid
					key = getKey(uuidGenerator.uuid(), parts[1]);
				} else {
					// Key id = refsetId (5th field) and referencedComponentId (6th field)
					String compositeKey = getCompositeKey(line);
					key = new StringKey(compositeKey);
					dirtyKeys.add(key);
				}
			} else {
				key = getKey(parts[0], parts[1]);
			}
			table.put(key, parts[2]);
		}
	}

	private String getCompositeKey(String line) throws DatabasePopulatorException {
		Matcher matcher = REFSET_ID_AND_REFERENCED_COMPONENT_ID_PATTERN.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new DatabasePopulatorException("No composite key match in line '" + line + "'");
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
