package org.ihtsdo.buildcloud.core.service.build.database.map;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableExportDAO;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableResults;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RF2TableExportDAOImpl implements RF2TableExportDAO {

	public static final Pattern REFSET_ID_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*).*");

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableExportDAOImpl.class);

	private final SchemaFactory schemaFactory;

	private Map<String, Pattern> refsetCompositeKeyPatternCache;

	private TableSchema tableSchema;

	private DataType idType;

	private Map<Key, String> table;

	private Map<StringKey,Key> dirtyKeys;

	private final ReferenceSetCompositeKeyPatternFactory refsetCompositeKeyPatternFactory;

	public RF2TableExportDAOImpl(final Map<String, List<Integer>> customRefsetCompositeKeys) {
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
		refsetCompositeKeyPatternCache = new HashMap<>();
		refsetCompositeKeyPatternFactory = new ReferenceSetCompositeKeyPatternFactory(customRefsetCompositeKeys);
	}

	@Override
	public TableSchema createTable(final String rf2FilePath, final InputStream rf2InputStream, final boolean workbenchDataFixesRequired) throws IOException, FileRecognitionException, DatabasePopulatorException, BadConfigurationException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			// Product Schema
			final String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			LOGGER.info("Creating table from {}", rf2Filename);
			final String headerLine = getHeader(rf2FilePath, reader);
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			if (tableSchema == null) {
				throw new FileRecognitionException("Failed to create a tableSchema using RF2 filename: " + rf2Filename);
			}
			idType = tableSchema.getFields().get(0).getType();
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader, tableSchema, true, workbenchDataFixesRequired);

			return tableSchema;
		}
	}

	@Override
	public void appendData(final TableSchema tableSchema, final Path rf2FilePath, final InputStream rf2InputStream, final boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException, BadConfigurationException {
		try (BufferedReader reader = rf2FilePath != null ? Files.newBufferedReader(rf2FilePath, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader, tableSchema, false, workbenchDataFixesRequired);
		}
	}

	@Override
	public void appendData(TableSchema tableSchema, Path rf2FilePath, InputStream rf2InputStream, String previousEffectiveDate) throws IOException, DatabasePopulatorException, BadConfigurationException {
		try (BufferedReader reader = rf2FilePath != null ? Files.newBufferedReader(rf2FilePath, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader, tableSchema, false, false, previousEffectiveDate);
		}
	}

	@Override
	public Set<Key> findAlreadyPublishedDeltaKeys(TableSchema tableSchema, Path previousSnapshotFilePath, InputStream previousSnapshotFileStream) throws IOException {
		if (this.table.isEmpty()) return Collections.emptySet();
		Set<Key> keysToDiscard = new HashSet<>();
		Map<String, List<Key>> idStringToKeyMap = new HashMap<>();
		for (Key key : table.keySet()) {
			idStringToKeyMap.computeIfAbsent(key.getIdString(),  k-> new ArrayList<>()).add(key);
		}
		try (BufferedReader reader = previousSnapshotFilePath != null ? Files.newBufferedReader(previousSnapshotFilePath, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			String line = reader.readLine(); // Discard header line
			String[] parts;
			String idString;
			while ((line = reader.readLine()) != null) {
				parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
				idString = tableSchema.getComponentType() == ComponentType.IDENTIFIER ? getIdentifierCompositeKey(line).getIdString() : getKey(parts[0], parts[1]).getIdString();
				if (!idStringToKeyMap.containsKey(idString)) {
					continue;
				}
				for (Key key : idStringToKeyMap.get(idString)) {
					if (Integer.parseInt(parts[1]) >= Integer.parseInt(key.getDate())) {
						keysToDiscard.add(key);
					}
				}
			}
		}
		return keysToDiscard;
	}

	@Override
	public RF2TableResults selectAllOrdered(final TableSchema tableSchema) {
		return new RF2TableResultsMapImpl(table);
	}

	@Override
	public RF2TableResults selectWithEffectiveDateOrdered(final TableSchema tableSchema, final String effectiveDate) {
		return new RF2TableResultsMapImpl(table, effectiveDate);
	}

	@Override
	public RF2TableResults selectNone(final TableSchema tableSchema) {
		return new RF2TableResultsMapImpl(new TreeMap<>());
	}

	@Override
	public void closeConnection() throws SQLException {
		if ( table != null) {
			table.clear();
			table = null;
		}
		
		if (dirtyKeys != null) {
			dirtyKeys.clear();
			dirtyKeys = null;
		}
		
		if (refsetCompositeKeyPatternCache != null) {
			refsetCompositeKeyPatternCache.clear();
			refsetCompositeKeyPatternCache = null;
		}
	}

	@Override
	public void discardAlreadyPublishedDeltaStates(final Path previousSnapshotFile, final InputStream previousSnapshotFileStream, final String currentSnapshotFileName, final String effectiveTime) throws IOException, DatabasePopulatorException {
		try (BufferedReader reader = previousSnapshotFile != null ? Files.newBufferedReader(previousSnapshotFile, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			getHeader(currentSnapshotFileName, reader);

			String line;
			String value;
			while ((line = reader.readLine()) != null) {
				final String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
				final Key key = getKey(parts[0], effectiveTime);
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
	public void reconcileRefsetMemberIds(final Path previousSnapshotFilePath, final InputStream previousSnapshotFileStream, final String currentSnapshotFileName, final String effectiveTime) throws IOException, DatabasePopulatorException, BadConfigurationException {
		LOGGER.info("Reconciling reference set member ids with previous published version of {}", currentSnapshotFileName);
		try (BufferedReader prevSnapshotReader = previousSnapshotFilePath != null ? Files.newBufferedReader(previousSnapshotFilePath, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			getHeader("previous to " + currentSnapshotFileName, prevSnapshotReader);

			String line, refsetId, compositeKey, newValues;
			String[] parts;
			Key matcherKey;
			Matcher refsetIdMatcher;
			Pattern keyPattern;
			while ((line = prevSnapshotReader.readLine()) != null) {
				// Get refset id
				refsetIdMatcher = REFSET_ID_PATTERN.matcher(line);
				if (refsetIdMatcher.matches()) {
					refsetId = refsetIdMatcher.group(1);
					keyPattern = getRefsetCompositeKeyPattern(tableSchema, refsetId);
					compositeKey = getCompositeKey(keyPattern, line);
					matcherKey = new StringKey(compositeKey);
					if ( dirtyKeys.get(matcherKey) != null) {
						newValues = table.get(dirtyKeys.get(matcherKey));
						if (newValues != null) {
							parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
							replaceDirtyKey(matcherKey, parts[0], effectiveTime);
						}
					}
				} else {
					throw new DatabasePopulatorException("Can't find refsetId id column");
				}
			}
		}
	}

	@Override
	public void resolveEmptyValueId(final Path previousFile, final InputStream previousSnapshotFileStream, final String effectiveTime) throws IOException {
		//check whether there are any empty value id
		final List<Key> emptyValueKeys = new ArrayList<>();
		for (final Key key : table.keySet()) {
			final String value = table.get(key);
			final String[] data = value.split(RF2Constants.COLUMN_SEPARATOR, -1);
			if (RF2Constants.EMPTY_SPACE.equals(data[data.length - 1])) {
				emptyValueKeys.add(key);
			}
		}
		LOGGER.info("Total number of rows with empty value id found: {}", emptyValueKeys.size());
		if (emptyValueKeys.isEmpty()) {
			//no empty value id is found.
			return;
		}
		try (BufferedReader reader = previousFile != null ? Files.newBufferedReader(previousFile, RF2Constants.UTF_8) : new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
			String line;
			line = reader.readLine();
			if (line == null) {
				throw new IOException("Privious attribute value snapshot file has no data");
			}
			while ((line = reader.readLine()) != null) {
				final String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
				final Key key = getKey(parts[0], effectiveTime);
				if (emptyValueKeys.contains(key)) {
					final String value = table.get(key);
					final boolean isPreviousActive = RF2Constants.BOOLEAN_TRUE.equals(parts[2]);
					//check data in delta file has got empty value id and with inactive flag
					final String[] data = value.split(RF2Constants.COLUMN_SEPARATOR, -1);
					final boolean isCurrentActive = RF2Constants.BOOLEAN_TRUE.equals(data[0]);
					if (!isCurrentActive) {
						if (isPreviousActive) {
							//add previous value id
							table.put(key, value + parts[6]);
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
		if (!emptyValueKeys.isEmpty()) {
			LOGGER.info("Found total number of rows with empty value id but member id doesn't exist in previous snapshot file: {}", emptyValueKeys.size());
		}
		for (final Key k : emptyValueKeys) {
			table.remove(k);
			dirtyKeys.remove(k);
		}
	}


	private void replaceDirtyKey(final Key existingDirtyKey, final String newKeyUUID, final String effectiveTime) throws DatabasePopulatorException {
		final String existingData = table.remove(dirtyKeys.get(existingDirtyKey));
		if (existingData != null) {
			table.put(new UUIDKey(newKeyUUID, effectiveTime), existingData);
			if (dirtyKeys.remove(existingDirtyKey) == null) {
				throw new DatabasePopulatorException("Failed to remove dirty key " + existingDirtyKey + "'");
			}
		} else {
			throw new DatabasePopulatorException("No match found when replacing dirty key '" + existingDirtyKey + "'");
		}
	}

	private String getHeader(final String rf2FilePath, final BufferedReader reader) throws IOException, DatabasePopulatorException {
		final String headerLine = reader.readLine();
		if (headerLine == null) {
			throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
		}
		return headerLine;
	}

	private void insertData(final BufferedReader reader, final TableSchema tableSchema, final boolean deltaData, final boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException, BadConfigurationException {
		insertData(reader, tableSchema, deltaData, workbenchDataFixesRequired, null);
	}

	private void insertData(final BufferedReader reader, final TableSchema tableSchema, final boolean deltaData, final boolean workbenchDataFixesRequired, String previousEffectiveDate) throws IOException, DatabasePopulatorException, BadConfigurationException {
		dirtyKeys = new LinkedHashMap<>();
		// Declare variables at top to prevent constant memory reallocation during recursion
		String line, refsetId, compositeKey;
		String[] parts;
		Key key;
		Matcher refsetIdMatcher;
		Pattern keyPattern;
		// date format is always in yyyyMMdd so it is faster to compare as integer
		Integer previousDate = previousEffectiveDate != null ? Integer.parseInt(previousEffectiveDate) : null;
		while ((line = reader.readLine()) != null) {
			parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			if (previousDate != null && Integer.parseInt(parts[1]) <= previousDate) {
				// skip data from previous release
				continue;
			}
			key = tableSchema.getComponentType() == ComponentType.IDENTIFIER ? getIdentifierCompositeKey(line) : getKey(parts[0], parts[1]);
			if (workbenchDataFixesRequired && deltaData && tableSchema.getComponentType() == ComponentType.REFSET) {
				// Get refset id
				refsetIdMatcher = REFSET_ID_PATTERN.matcher(line);
				if (refsetIdMatcher.matches()) {
					refsetId = refsetIdMatcher.group(1);
					keyPattern = getRefsetCompositeKeyPattern(tableSchema, refsetId);
					compositeKey = getCompositeKey(keyPattern, line);
					StringKey composite = new StringKey(compositeKey);
					if (dirtyKeys.containsKey(composite)) {
						LOGGER.info(RF2Constants.DATA_PROBLEM + "Duplicate refset member found. Rows are logically equivalent, the first one will be discarded: [{}], [{}].",
								table.get(dirtyKeys.get(composite)), parts[2]);
						table.remove(dirtyKeys.get(composite));
					} 
					dirtyKeys.put(composite, key);
				} else {
					throw new DatabasePopulatorException("Can't find refsetId id column");
				}
			} 
			table.put(key, parts[2]);
		}
	}

	private Pattern getRefsetCompositeKeyPattern(final TableSchema tableSchema, final String refsetId) throws BadConfigurationException {
		if (tableSchema.getComponentType() == ComponentType.REFSET) {
			if (!refsetCompositeKeyPatternCache.containsKey(refsetId)) {
				refsetCompositeKeyPatternCache.put(refsetId, refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, refsetId));
			}
			return refsetCompositeKeyPatternCache.get(refsetId);
		} else {
			return null;
		}
	}

	private String getCompositeKey(final Pattern keyPattern, final String line) throws DatabasePopulatorException {
		final Matcher matcher = keyPattern.matcher(line);
		if (matcher.matches()) {
			StringBuilder key = new StringBuilder();
			for (int a = 0; a < matcher.groupCount(); a++) {
				key.append(matcher.group(a + 1));
			}
			return key.toString();
		} else {
			throw new DatabasePopulatorException("No composite key match in line '" + line + "'");
		}
	}

	private Key getIdentifierCompositeKey(final String line) {
		String[] parts = line.split(RF2Constants.COLUMN_SEPARATOR, 6);
		return new StringKey(parts[0] + RF2Constants.COLUMN_SEPARATOR + parts[4], parts[1]);
	}

	private Key getKey(final String part0, final String part1) {
		Key key;
		if (idType == DataType.SCTID) {
			key = new SCTIDKey(part0, part1);
		} else {
			key = new UUIDKey(part0, part1);
		}
		return key;
	}
}
