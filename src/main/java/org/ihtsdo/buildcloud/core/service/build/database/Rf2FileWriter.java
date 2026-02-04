package org.ihtsdo.buildcloud.core.service.build.database;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.map.Key;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Rf2FileWriter {

	private String excludeRefsetDescriptorMembers;

	private String excludeLanguageRefsetIds;

	public Rf2FileWriter() {}

	public Rf2FileWriter(final String excludeRefsetDescriptorMembers, final String excludeLanguageRefsetIds) {
		this.excludeRefsetDescriptorMembers = excludeRefsetDescriptorMembers;
		this.excludeLanguageRefsetIds = excludeLanguageRefsetIds;
	}

	public void exportDelta(RF2TableResults tableResults, TableSchema tableSchema, OutputStream deltaOutputStream, Set<Key> deltaKeysToDiscard) throws SQLException, IOException {
		// Pre-index keys once to avoid O(rows * keysToDiscard) linear scans.
		final Set<String> ignoredKeyLookup = buildIgnoredKeyLookup(deltaKeysToDiscard);
		final boolean isIdentifier = ComponentType.IDENTIFIER.equals(tableSchema.getComponentType());
		final boolean isLanguageFile = !isIdentifier && Pattern.compile(RF2Constants.LANGUAGE_FILE_PATTERN).matcher(tableSchema.getFilename()).matches();

		try (BufferedWriter deltaWriter = new BufferedWriter(new OutputStreamWriter(deltaOutputStream, RF2Constants.UTF_8))) {
			List<Field> fields = tableSchema.getFields();
			// Write header
			String header = productHeader(fields);
			deltaWriter.write(header);
			deltaWriter.append(RF2Constants.LINE_ENDING);

			String line;
			String currentId;
			while ((line = tableResults.nextLine()) != null) {
				String[] lineParts;
				String languageRefsetId = null;
				if (isIdentifier) {
					lineParts = line.split(RF2Constants.COLUMN_SEPARATOR, 6);
					currentId = lineParts[0] + RF2Constants.COLUMN_SEPARATOR + lineParts[1];
					// Replace the composite key by identifierSchemeId
					line = line.replace(currentId, lineParts[0]);
				} else {
					if (isLanguageFile) {
						lineParts = line.split(RF2Constants.COLUMN_SEPARATOR, 6);
						languageRefsetId = lineParts[4];
					} else {
						lineParts = line.split(RF2Constants.COLUMN_SEPARATOR, 3);
					}
					currentId = lineParts[0];
				}

				final String effectiveTime = isIdentifier ? lineParts[2] : lineParts[1];
				if (isIgnoredKey(ignoredKeyLookup, currentId, effectiveTime)
				 || (!isIdentifier && isRF2LineExcluded(tableSchema, currentId, languageRefsetId))) {
					continue;
				}
				deltaWriter.append(line);
				deltaWriter.append(RF2Constants.LINE_ENDING);
			}
		}
	}

	private static Set<String> buildIgnoredKeyLookup(Set<Key> deltaKeysToDiscard) {
		if (deltaKeysToDiscard == null || deltaKeysToDiscard.isEmpty()) {
			return Collections.emptySet();
		}
		// Use a delimiter that cannot appear in tab-separated RF2 ids to avoid collisions.
		final int capacity = (int) (deltaKeysToDiscard.size() / 0.75f) + 1;
		Set<String> ignoredKeys = new HashSet<>(capacity);
		for (Key key : deltaKeysToDiscard) {
			if (key == null || key.getIdString() == null || key.getDate() == null) {
				continue;
			}
			ignoredKeys.add(composeIgnoredKey(key.getIdString(), key.getDate()));
		}
		return ignoredKeys;
	}

	private static boolean isIgnoredKey(Set<String> ignoredKeyLookup, String currentId, String effectiveTime) {
		if (ignoredKeyLookup.isEmpty()) {
			return false;
		}
		return ignoredKeyLookup.contains(composeIgnoredKey(currentId, effectiveTime));
	}

	private static String composeIgnoredKey(String id, String effectiveTime) {
		return id + '\u0000' + effectiveTime;
	}

	public void exportFullAndSnapshot(RF2TableResults tableResults, TableSchema schema, Date targetEffectiveTime, OutputStream fullOutputStream, OutputStream snapshotOutputStream) throws SQLException, IOException {

		try (BufferedWriter fullWriter = new BufferedWriter(new OutputStreamWriter(fullOutputStream, RF2Constants.UTF_8));
			 BufferedWriter snapshotWriter = new BufferedWriter(new OutputStreamWriter(snapshotOutputStream, RF2Constants.UTF_8))) {

			// Declare a few objects to reuse over and over.
			final List<Field> fields = schema.getFields();

			// Product header
			String header = productHeader(fields);
			fullWriter.write(header);
			fullWriter.append(RF2Constants.LINE_ENDING);
			snapshotWriter.write(header);
			snapshotWriter.append(RF2Constants.LINE_ENDING);

			// Variables for snapshot resolution
			String currentLine;
			String currentId;
			int currentEffectiveTimeInt;
			int targetEffectiveTimeInt = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(targetEffectiveTime));
			String lastId = null;
			String validLine = null;
			boolean movedToNewMember;
			boolean passedTargetEffectiveTime;

			// Iterate through data
			while ((currentLine = tableResults.nextLine()) != null) {
				// Parse out id and effectiveTime
				String[] lineParts;
				String languageRefsetId = null;
				if (ComponentType.IDENTIFIER.equals(schema.getComponentType())) {
					lineParts = currentLine.split(RF2Constants.COLUMN_SEPARATOR, 6);
					currentId = lineParts[0] + RF2Constants.COLUMN_SEPARATOR + lineParts[1];
					// effective time is on the third column
					currentEffectiveTimeInt = Integer.parseInt(lineParts[2]);
					// Replace the composite key by identifierSchemeId
					currentLine = currentLine.replace(currentId, lineParts[0]);
				} else {
					if (Pattern.compile(RF2Constants.LANGUAGE_FILE_PATTERN).matcher(schema.getFilename()).matches()) {
						lineParts = currentLine.split(RF2Constants.COLUMN_SEPARATOR, 6);
						languageRefsetId = lineParts[4];
					} else {
						lineParts = currentLine.split(RF2Constants.COLUMN_SEPARATOR, 3);
					}

					currentId = lineParts[0];
					// effective time is on the second column
					currentEffectiveTimeInt = Integer.parseInt(lineParts[1]);

					if (isRF2LineExcluded(schema, currentId, languageRefsetId)) {
						continue;
					}
				}

				// Write to Full file
				fullWriter.append(currentLine);
				fullWriter.append(RF2Constants.LINE_ENDING);

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
			String header = productHeader(tableSchema.getFields());
			fullWriter.append(header);
			fullWriter.append(RF2Constants.LINE_ENDING);

			String line;
			while ((line = results.nextLine()) != null) {
				fullWriter.append(line);
				fullWriter.append(RF2Constants.LINE_ENDING);
			}
		}
	}

	private boolean isRF2LineExcluded(TableSchema tableSchema, String currentId, String languageRefsetId) {
		return (tableSchema.getFilename().contains(RF2Constants.REFERENCE_SET_DESCRIPTOR_FILE_IDENTIFIER) && this.excludeRefsetDescriptorMembers != null && this.excludeRefsetDescriptorMembers.contains(currentId))
				|| (this.excludeLanguageRefsetIds != null && languageRefsetId != null && excludeLanguageRefsetIds.contains(languageRefsetId));
	}

	private String productHeader(List<Field> fields) {
		StringBuilder producter = new StringBuilder();
		boolean firstField = true;
		for (Field field : fields) {
			if (firstField) {
				firstField = false;
			} else {
				producter.append(RF2Constants.COLUMN_SEPARATOR);
			}
			producter.append(field.getName());
		}
		return producter.toString();
	}
}
