package org.ihtsdo.buildcloud.service.execution.database.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.execution.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RF2TableDAOTreeMapImpl implements RF2TableDAO {

	private final SchemaFactory schemaFactory;
	private TableSchema tableSchema;
	private DataType idType;
	private Map<Key, String> table;
	private Set<Key> dirtyKeys;
	private final UUIDGenerator uuidGenerator;

	private static final Pattern REFSET_ID_AND_REFERENCED_COMPONENT_ID_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*\t[^\t]*)(\t.*)?");
	private static final Pattern REFSET_ID_REFERENCED_COMPONENT_ID_AND_TARGET_ID_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*\t[^\t]*)(\t.*)?");
	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableDAOTreeMapImpl.class);

	public RF2TableDAOTreeMapImpl(UUIDGenerator uuidGenerator) {
		this.uuidGenerator = uuidGenerator;
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
	}

	@Override
	public TableSchema createTable(String rf2FilePath, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			// Build Schema
			String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			String headerLine = getHeader(rf2FilePath, reader);
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			idType = tableSchema.getFields().get(0).getType();
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader, tableSchema, true, workbenchDataFixesRequired);

			return tableSchema;
		}

	}

	@Override
	public void appendData(TableSchema tableSchema, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws IOException, SQLException, ParseException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader, tableSchema, false, workbenchDataFixesRequired);
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
						LOGGER.debug("Removing already published Delta state in {} : {}", currentSnapshotFileName, line);
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
				String compositeKey = getCompositeKey(tableSchema, line);
				Key matcherKey = new StringKey(compositeKey);
				String newValues = table.get(matcherKey);
				if (newValues != null) {
					parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
					replaceDirtyKey(matcherKey, parts[0], effectiveTime);
				}
			}
		}
	}

	@Override
	public void generateNewMemberIds(String effectiveTime) throws DatabasePopulatorException {
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

	private void insertData(BufferedReader reader, TableSchema tableSchema, boolean deltaData, boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException {
		String line;
		String[] parts;
		Key key;
		dirtyKeys = new LinkedHashSet<>();
		while ((line = reader.readLine()) != null) {
			parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			if (workbenchDataFixesRequired && deltaData && tableSchema.getComponentType() == ComponentType.REFSET) {
				// Key id = refsetId (5th field) and referencedComponentId (6th field)
				String compositeKey = getCompositeKey(tableSchema, line);
				key = new StringKey(compositeKey);
				dirtyKeys.add(key);
			} else {
				key = getKey(parts[0], parts[1]);
			}
			table.put(key, parts[2]);
		}
	}

	private String getCompositeKey(TableSchema tableSchema, String line) throws DatabasePopulatorException {
		Pattern compKeyPattern;
		List<Field> fields = tableSchema.getFields();
		if (fields.size() >= 7 && ("mapTarget".equals(fields.get(6).getName()) || "targetComponentId".equals(fields.get(6).getName()))) {
			// Simple Map or Association
			compKeyPattern = REFSET_ID_REFERENCED_COMPONENT_ID_AND_TARGET_ID_PATTERN;
		} else {
			// Simple RefSet
			compKeyPattern = REFSET_ID_AND_REFERENCED_COMPONENT_ID_PATTERN;
		}
		Matcher matcher = compKeyPattern.matcher(line);
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

	@Override
	public void resolveEmptyValueId(final InputStream previousSnapshotFileStream, final String effectiveTime) throws IOException {
		//check whether there are any empty value id
		List<Key> emptyValueKeys = new ArrayList<>();
		for (Key key : table.keySet()) {
			String value = table.get(key);
			String[] data = value.split(RF2Constants.COLUMN_SEPARATOR, -1);
			if (RF2Constants.EMPTY_SPACE.equals(data[data.length - 1])) {
				emptyValueKeys.add(key);
			}
		}
		LOGGER.info("Total number of rows with empty value id found:" + emptyValueKeys.size());
		if (emptyValueKeys.size() < 1) {
			//no empty value id is found.
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			String line;
			line = reader.readLine();
			if (line == null) {
				throw new IOException("Privious attribute value snapshot file has no data");
			}
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
				Key key = getKey(parts[0], effectiveTime);
				if (emptyValueKeys.contains(key)) {
					String value = table.get(key);
					boolean isPreviousActive = RF2Constants.BOOLEAN_TRUE.equals(parts[2]);
					//check data in delta file has got empty value id and with inactive flag
					String[] data = value.split(RF2Constants.COLUMN_SEPARATOR, -1);
					boolean isCurrentActive = RF2Constants.BOOLEAN_TRUE.equals(data[0]);
					if (!isCurrentActive) {
						if (isPreviousActive) {
							//add previous value id
							StringBuilder updated = new StringBuilder(value);
							updated.append(parts[6]);
							table.put(key, updated.toString());
						} else {
							//remove line from table and dirty key set
							table.remove(key);
							dirtyKeys.remove(key);
						}
					}
					emptyValueKeys.remove(key);
				}
			}
		}
		//remove any rows with empty value id as not existing in previous file
		if (emptyValueKeys.size() > 0) {
			LOGGER.info("Found total number of rows with empty value id but member id doesn't exist in previous snapshot file:" + emptyValueKeys.size());
		}
		for (Key k : emptyValueKeys) {
			table.remove(k);
			dirtyKeys.remove(k);
		}
	}
}
