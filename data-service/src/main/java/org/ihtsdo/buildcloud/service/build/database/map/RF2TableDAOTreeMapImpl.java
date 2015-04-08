package org.ihtsdo.buildcloud.service.build.database.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.build.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.build.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.otf.utils.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: This class wants renaming to RF2TableExport .. we should probably remove the Hsql implementation as it's not used and out of date.
public class RF2TableDAOTreeMapImpl implements RF2TableDAO {

	public static final Pattern REFSET_ID_PATTERN = Pattern.compile("[^\t]*\t[^\t]*\t[^\t]*\t[^\t]*\t([^\t]*).*");

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2TableDAOTreeMapImpl.class);

	private final SchemaFactory schemaFactory;

	private final Map<String, List<Integer>> customRefsetCompositeKeys;

	private final UUIDGenerator uuidGenerator;

	private final Map<String, Pattern> refsetCompositeKeyPatternCache;

	private TableSchema tableSchema;

	private DataType idType;

	private Map<Key, String> table;

	private Set<Key> dirtyKeys;

	private final ReferenceSetCompositeKeyPatternFactory refsetCompositeKeyPatternFactory;

	public RF2TableDAOTreeMapImpl(final UUIDGenerator uuidGenerator, final Map<String, List<Integer>> customRefsetCompositeKeys) {
		this.uuidGenerator = uuidGenerator;
		schemaFactory = new SchemaFactory();
		table = new TreeMap<>();
		this.customRefsetCompositeKeys = customRefsetCompositeKeys;
		refsetCompositeKeyPatternCache = new HashMap<>();
		refsetCompositeKeyPatternFactory = new ReferenceSetCompositeKeyPatternFactory(this.customRefsetCompositeKeys);
	}

	@Override
	public TableSchema createTable(final String rf2FilePath, final InputStream rf2InputStream, final boolean workbenchDataFixesRequired) throws IOException, FileRecognitionException, DatabasePopulatorException, BadConfigurationException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			// Product Schema
			final String rf2Filename = FileUtils.getFilenameFromPath(rf2FilePath);
			LOGGER.info("Creating table from {}", rf2Filename);
			final String headerLine = getHeader(rf2FilePath, reader);
			tableSchema = schemaFactory.createSchemaBean(rf2Filename);
			idType = tableSchema.getFields().get(0).getType();
			schemaFactory.populateExtendedRefsetAdditionalFieldNames(tableSchema, headerLine);

			// Insert Data
			insertData(reader, tableSchema, true, workbenchDataFixesRequired);

			return tableSchema;
		}
	}

	@Override
	public void appendData(final TableSchema tableSchema, final InputStream rf2InputStream, final boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException, BadConfigurationException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2InputStream, RF2Constants.UTF_8))) {
			reader.readLine(); // Discard header line
			insertData(reader, tableSchema, false, workbenchDataFixesRequired);
		}
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
		return new RF2TableResultsMapImpl(new TreeMap<Key, String>());
	}

	@Override
	public void closeConnection() throws SQLException {
		table = null;
	}

	@Override
	public void discardAlreadyPublishedDeltaStates(final InputStream previousSnapshotFileStream, final String currentSnapshotFileName, final String effectiveTime) throws IOException, DatabasePopulatorException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
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
	public void reconcileRefsetMemberIds(final InputStream previousSnapshotFileStream, final String currentSnapshotFileName, final String effectiveTime) throws IOException, DatabasePopulatorException, BadConfigurationException {
		LOGGER.info("Reconciling reference set member ids with previous published version of {}", currentSnapshotFileName);
		try (BufferedReader prevSnapshotReader = new BufferedReader(new InputStreamReader(previousSnapshotFileStream, RF2Constants.UTF_8))) {
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
					newValues = table.get(matcherKey);
					if (newValues != null) {
						parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
						replaceDirtyKey(matcherKey, parts[0], effectiveTime);
					}
				} else {
					throw new DatabasePopulatorException("Can't find refsetId id column");
				}
			}
		}
	}

	@Override
	public void resolveEmptyValueId(final InputStream previousSnapshotFileStream, final String effectiveTime) throws IOException {
		//check whether there are any empty value id
		final List<Key> emptyValueKeys = new ArrayList<>();
		for (final Key key : table.keySet()) {
			final String value = table.get(key);
			final String[] data = value.split(RF2Constants.COLUMN_SEPARATOR, -1);
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
		if (emptyValueKeys.size() > 0) {
			LOGGER.info("Found total number of rows with empty value id but member id doesn't exist in previous snapshot file:" + emptyValueKeys.size());
		}
		for (final Key k : emptyValueKeys) {
			table.remove(k);
			dirtyKeys.remove(k);
		}
	}

	@Override
	public void generateNewMemberIds(final String effectiveTime) throws DatabasePopulatorException {
		if (!dirtyKeys.isEmpty()) {
			LOGGER.info("{} dirty refset ids remain. Generating UUIDs for new members.", dirtyKeys.size());
			while (dirtyKeys.iterator().hasNext()) {
				final Key remainingDirtyKey = dirtyKeys.iterator().next();
				final String newKey = uuidGenerator.uuid();
				replaceDirtyKey(remainingDirtyKey, newKey, effectiveTime);
			}
		} else {
			LOGGER.info("No dirty refset ids remain. No new members.", dirtyKeys.size());
		}
	}

	private void replaceDirtyKey(final Key existingDirtyKey, final String newKeyUUID, final String effectiveTime) throws DatabasePopulatorException {
		final String existingData = table.remove(existingDirtyKey);
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

	private String getHeader(final String rf2FilePath, final BufferedReader reader) throws IOException, DatabasePopulatorException {
		final String headerLine = reader.readLine();
		if (headerLine == null) {
			throw new DatabasePopulatorException("RF2 file " + rf2FilePath + " is empty.");
		}
		return headerLine;
	}

	private void insertData(final BufferedReader reader, final TableSchema tableSchema, final boolean deltaData, final boolean workbenchDataFixesRequired) throws IOException, DatabasePopulatorException, BadConfigurationException {
		dirtyKeys = new LinkedHashSet<>();
		// Declare variables at top to prevent constant memory reallocation during recursion
		String line, refsetId, compositeKey;
		String[] parts;
		Key key;
		Matcher refsetIdMatcher;
		Pattern keyPattern;
		while ((line = reader.readLine()) != null) {
			parts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
			if (workbenchDataFixesRequired && deltaData && tableSchema.getComponentType() == ComponentType.REFSET) {
				// Get refset id
				refsetIdMatcher = REFSET_ID_PATTERN.matcher(line);
				if (refsetIdMatcher.matches()) {
					refsetId = refsetIdMatcher.group(1);
					keyPattern = getRefsetCompositeKeyPattern(tableSchema, refsetId);
					compositeKey = getCompositeKey(keyPattern, line);
					key = new StringKey(compositeKey);
					if (!dirtyKeys.contains(key)) {
						dirtyKeys.add(key);
					} else {
						LOGGER.info(RF2Constants.DATA_PROBLEM + "Duplicate refset member found. Rows are logically equivalent, the first will be discarded: [{}], [{}].", table.get(key), parts[2]);
					}
				} else {
					throw new DatabasePopulatorException("Can't find refsetId id column");
				}
			} else {
				key = getKey(parts[0], parts[1]);
			}
			table.put(key, parts[2]);
		}
	}

	private Pattern getRefsetCompositeKeyPattern(final TableSchema tableSchema, final String refsetId) throws DatabasePopulatorException, BadConfigurationException {
		if (tableSchema.getComponentType() == ComponentType.REFSET) {
			if (!refsetCompositeKeyPatternCache.containsKey(refsetId)) {
				refsetCompositeKeyPatternCache.put(refsetId, refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, refsetId));
			}
			return refsetCompositeKeyPatternCache.get(refsetId);
		} else {
			return null;
		}
	}

	private Pattern productRefsetCompositeKeyPattern(final TableSchema tableSchema, final String refsetId) throws BadConfigurationException {
		final Set<Integer> fieldIndexes = new TreeSet<>();

		final List<Field> fields = tableSchema.getFields();
		fieldIndexes.add(4); // refsetId is a must

		boolean customCompositeKey = false;
		if (customRefsetCompositeKeys.containsKey(refsetId)) {
			customCompositeKey = true;
			fieldIndexes.addAll(customRefsetCompositeKeys.get(refsetId));
		} else {
			if (fields.size() == 13 && "mapPriority".equals(fields.get(7).getName())) {
				// Extended Map
				fieldIndexes.add(5);
				fieldIndexes.add(7);
				fieldIndexes.add(10);
			} else if (fields.size() == 9 && "attributeOrder".equals(fields.get(8).getName())) {
				// Reference Set Descriptor - need the attributeOrder to make the row unique
				fieldIndexes.add(5);
				fieldIndexes.add(8);
			} else if (fields.size() >= 7
					&& ("mapTarget".equals(fields.get(6).getName()) || "targetComponentId".equals(fields.get(6).getName()))) {
				// Simple Map or Association
				fieldIndexes.add(5);
				fieldIndexes.add(6);
			} else if (fields.size() == 8 && fields.get(6).getName().equals("sourceEffectiveTime") && fields.get(7).getName().equals("targetEffectiveTime")) {
				// Module Dependency
				// id	effectiveTime	active	moduleId	[refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime]
				fieldIndexes.add(5);
				fieldIndexes.add(6);
				fieldIndexes.add(7);
			} else {
				// Simple RefSet
				fieldIndexes.add(5);
			}
		}

		final int maxFieldIndex = fields.size() - 1;
		final Integer maxConfiguredFieldIndex = Collections.max(fieldIndexes);
		if (maxConfiguredFieldIndex > maxFieldIndex) {
			throw new BadConfigurationException("Reference set composite key index " + maxConfiguredFieldIndex + " is out of bounds for file " + tableSchema.getFilename() + ".");
		}

		final List<String> fieldNames = new ArrayList<>();
		String patternString = "";
		boolean alreadyMatchingTab = false;
		for (int a = 0; !fieldIndexes.isEmpty(); a++) {
			if (a > 0) {
				if (alreadyMatchingTab) {
					alreadyMatchingTab = false;
				} else {
					patternString += "\t";
				}
			}
 			if (!fieldIndexes.contains(a)) {
				patternString += "[^\t]*";
			} else {
				fieldIndexes.remove(a);
				fieldNames.add(a + " (" + fields.get(a).getName() + ")");
				patternString += "([^\t]*\t?)";
				alreadyMatchingTab = true;
			}
		}
		patternString += ".*";

		LOGGER.info("Reference Set {} in {} using {} composite key with fields {}, pattern {}", refsetId, tableSchema.getFilename(), customCompositeKey ? "custom" : "standard", fieldNames, patternString);
		return Pattern.compile(patternString);
	}

	private String getCompositeKey(final Pattern keyPattern, final String line) throws DatabasePopulatorException {
		final Matcher matcher = keyPattern.matcher(line);
		if (matcher.matches()) {
			String key = "";
			for (int a = 0; a < matcher.groupCount(); a++) {
				key += matcher.group(a + 1);
			}
			return key;
		} else {
			throw new DatabasePopulatorException("No composite key match in line '" + line + "'");
		}
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
