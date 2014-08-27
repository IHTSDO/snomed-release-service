package org.ihtsdo.buildcloud.service.workbenchdatafix;

import org.ihtsdo.buildcloud.service.exception.BadInputFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModuleResolverService {

	private final Pattern existingFileRowPattern;
	private final Pattern inputFileRowPattern;

	private static final Logger LOGGER = LoggerFactory.getLogger(ModuleResolverService.class);

	public ModuleResolverService(String isASctid, String modelModuleSctid) {
		// RF2 Relationship fields:
		// id effectiveTime active moduleId sourceId destinationId relationshipGroup typeId characteristicTypeId modifierId

		// Pattern for matching row which is active with a moduleId of model and a typeId of isA. Group for sourceId extraction.
		existingFileRowPattern = Pattern.compile("[^\t]*\t[^\t]*\t1\t" + modelModuleSctid + "\t([^\t]*)\t[^\t]*\t[^\t]*\t" + isASctid + "\t.*");

		// Pattern for matching row which is active with a typeId of isA.
		// We can't match on moduleId here because it's wrong in input files coming out of workbench.
		// Groups for sourceId and destinationId extraction.
		inputFileRowPattern = Pattern.compile("[^\t]*\t[^\t]*\t1\t[^\t]*\t([^\t]*)\t([^\t]*)\t[^\t]*\t" + isASctid + "\t.*");
	}

	/**
	 * Extracts a set of concept IDs (sourceId field) from a stated relationship snapshot output file where the moduleId is the
	 * model module id.
	 */
	public Set<String> getExistingModelConceptIds(InputStream previousStatedRelationshipSnapshot) throws BadInputFileException {
		Set<String> snapshotModelConceptIds = new HashSet<>();

		try (BufferedReader snapshotReader = new BufferedReader(new InputStreamReader(previousStatedRelationshipSnapshot))) {
			String line = snapshotReader.readLine();
			if (line != null) {
				while ((line = snapshotReader.readLine()) != null) {
					Matcher matcher = existingFileRowPattern.matcher(line);
					if (matcher.matches()) {
						snapshotModelConceptIds.add(matcher.group(1));
					}
				}
			} else {
				throw new BadInputFileException("Previous stated relationship snapshot is empty.");
			}
		} catch (IOException e) {
			LOGGER.error("Error reading a stated relationship stream.", e);
		}

		return snapshotModelConceptIds;
	}

	/**
	 * Extracts concept IDs (sourceId field) from a stated relationship delta input file where the moduleId is the
	 * model module id. Extracted IDs are added to the existing set. Relationship rows can be in any order.
	 */
	public void addNewModelConceptIds(Set<String> modelConceptIds, InputStream inputStatedRelationshipDelta) throws BadInputFileException {
		Map<String, List<String>> destinationIdToSourceIdListMap = new LinkedHashMap<>();

		// Build destination id > source ids map
		try (BufferedReader deltaReader = new BufferedReader(new InputStreamReader(inputStatedRelationshipDelta))) {
			String line = deltaReader.readLine();
			if (line != null) {
				while ((line = deltaReader.readLine()) != null) {
					Matcher matcher = inputFileRowPattern.matcher(line);
					if (matcher.matches()) {
						String sourceId = matcher.group(1);
						String destinationId = matcher.group(2);
						if (!destinationIdToSourceIdListMap.containsKey(destinationId)) {
							destinationIdToSourceIdListMap.put(destinationId, new ArrayList<String>());
						}
						destinationIdToSourceIdListMap.get(destinationId).add(sourceId);
					}
				}
			} else {
				throw new BadInputFileException("Input stated relationship delta is empty.");
			}
		} catch (IOException e) {
			LOGGER.error("Error reading a stated relationship stream.", e);
		}

		// Use map to get source ids where destination is a model concept
		Set<String> findModelConceptIds = modelConceptIds;
		Set<String> newModelConceptIds = new HashSet<>();
		boolean keepProcessing = true;
		while (keepProcessing) {
			for (String destinationId : destinationIdToSourceIdListMap.keySet()) {
				if (findModelConceptIds.contains(destinationId)) {
					// Add all sourceIds which point to this destination
					newModelConceptIds.addAll(destinationIdToSourceIdListMap.get(destinationId));
				}
			}

			if (newModelConceptIds.isEmpty()) {
				keepProcessing = false;
			} else {
				// Store new concept ids
				modelConceptIds.addAll(newModelConceptIds);

				// Repeat map recursion tracing relationships of newly discovered model concepts
				findModelConceptIds = newModelConceptIds;

				newModelConceptIds = new HashSet<>();
			}
		}

	}
}
