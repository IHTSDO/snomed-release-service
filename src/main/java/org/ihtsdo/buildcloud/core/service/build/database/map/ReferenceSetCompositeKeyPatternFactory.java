package org.ihtsdo.buildcloud.core.service.build.database.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceSetCompositeKeyPatternFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceSetCompositeKeyPatternFactory.class);
	private final Map<String, List<Integer>> customRefsetCompositeKeys;
	
	public ReferenceSetCompositeKeyPatternFactory(final Map<String, List<Integer>> customRefsetCompositeKeyMap) {
		customRefsetCompositeKeys = customRefsetCompositeKeyMap;
	}

	public Pattern getRefsetCompositeKeyPattern(final TableSchema tableSchema, final String refsetId) throws BadConfigurationException {
		final Set<Integer> fieldIndexes = new TreeSet<>();
		final List<Field> fields = tableSchema.getFields();
		fieldIndexes.add(4); // refsetId is a must
		boolean customCompositeKey = false;
		if (customRefsetCompositeKeys.containsKey(refsetId)) {
			customCompositeKey = true;
			fieldIndexes.addAll(customRefsetCompositeKeys.get(refsetId));
		} else {
			final String fileName = tableSchema.getFilename();
			if (fileName.contains(RF2Constants.EXTENEDED_MAP_FILE_IDENTIFIER) || fileName.contains(RF2Constants.COMPLEX_MAP_FILE_IDENTIFIER)) {
				fieldIndexes.add(5);
				fieldIndexes.add(7);
				fieldIndexes.add(10);
			} else if (fileName.contains(RF2Constants.REFERENCE_SET_DESCRIPTOR_FILE_IDENTIFIER)) {
				// Reference Set Descriptor - need the attributeOrder to make the row unique
				fieldIndexes.add(5);
				fieldIndexes.add(8);
			} else if (fileName.contains(RF2Constants.SIMPLE_MAP_FILE_IDENTIFIER) || fileName.contains(RF2Constants.ASSOCIATION_REFERENCE_FILE_IDENTIFIER)) {
				fieldIndexes.add(5);
				fieldIndexes.add(6);
			} else if (fileName.contains(RF2Constants.MODULE_DEPENDENCY_FILE_IDENTIFIER)) {
				// Module Dependency
				// id	effectiveTime	active	moduleId	[refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime]
				fieldIndexes.add(5);
				fieldIndexes.add(6);
				fieldIndexes.add(7);
			} else {
				// Simple RefSet or Description type
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
}
