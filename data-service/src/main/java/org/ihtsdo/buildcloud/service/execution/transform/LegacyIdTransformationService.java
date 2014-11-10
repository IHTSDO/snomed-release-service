package org.ihtsdo.buildcloud.service.execution.transform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class LegacyIdTransformationService {
	private static final String STATED_RELATIONSHIP_DELTA_FILE_PREFIX = "sct2_StatedRelationship_Delta_INT_";
	private static final String REFSET_SIMPLE_MAP_DELTA_FILE_PREFIX = "der2_sRefset_SimpleMapDelta_INT_";
	private static final String SNOMED_ID_REFSET_ID = "900000000000498005";
	private static final String CTV3_ID_REFSET_ID = "900000000000497000";
	private static final String TAB = "\t";
	private static final Logger LOGGER = LoggerFactory.getLogger(LegacyIdTransformationService.class);
	@Autowired
	private IdAssignmentBI idAssignmentBI;
	@Autowired
	private UUIDGenerator uuidGenerator;
	@Autowired
	private ExecutionDAO executionDAO;
	
	public void transformLegacyIds(final CachedSctidFactory cachedSctidFactory, final Map<String, List<UUID>> moduleIdAndUuidMap, final Execution execution) throws TransformationException {
		final List<UUID> newConceptUuids   = new ArrayList<>();
		for (final String moduleId : moduleIdAndUuidMap.keySet()) {
			newConceptUuids.addAll(moduleIdAndUuidMap.get(moduleId));
		}
		LOGGER.info("Total new concepts:" + newConceptUuids.size());
		
		final String packageBusinessKey = execution.getBuild().getBusinessKey();
		final String effectiveDate = execution.getBuild().getEffectiveTimeSnomedFormat();
		final String simpleRefsetMapDelta = REFSET_SIMPLE_MAP_DELTA_FILE_PREFIX + effectiveDate + RF2Constants.TXT_FILE_EXTENSION;
		final InputStream inputStream = executionDAO.getTransformedFileAsInputStream(execution, packageBusinessKey, simpleRefsetMapDelta);
		if (inputStream == null) {
			LOGGER.info("No transformed file found for " + simpleRefsetMapDelta);
			return;
			//no need to generate legacy id mapping as maybe simple Refset map is not in the manifest file.
		}
		final List<Long> sctIds = new ArrayList<>();
		for (final UUID uuid : newConceptUuids) {
			final Long sctId = cachedSctidFactory.getSCTIDFromCache(uuid.toString());
			if (sctId != null) {
				LOGGER.debug("SctId:" + sctId + " UUID:" + uuid.toString());
				sctIds.add(sctId); 
				} else {
				LOGGER.error("Failed to find sctId from cache for UUID:" +  uuid.toString());
			}
		}

		//can't append to existing file so need to read them into memory and write again along with additional data.
		final List<String> existingLines = new ArrayList<>();
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				existingLines.add(line);
			}
		} catch (final IOException e) {
			throw new TransformationException("Error occurred when reading simple refset map delta transformed file:" + simpleRefsetMapDelta, e);
		}
		
		try (
			final OutputStream outputStream = executionDAO.getTransformedFileOutputStream(execution, packageBusinessKey, simpleRefsetMapDelta).getOutputStream();
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, RF2Constants.UTF_8))) {
			if (!moduleIdAndUuidMap.isEmpty()) {
				for (final String existingLine : existingLines) {
					writer.write(existingLine);
					writer.write(RF2Constants.LINE_ENDING);
				}
				//Generate CTV3 ID
				LOGGER.info("Start CTV3ID generation");
				final LegacyIdGenerator idGenerator = new LegacyIdGenerator(idAssignmentBI);
				final Map<UUID, String> uuidCtv3IdMap = idGenerator.generateCTV3IDs(newConceptUuids);
				LOGGER.info("Created ctv3Ids for new concept ids found: " + uuidCtv3IdMap.size());
				//generate snomed id
				final Map<Long, Long> sctIdAndParentMap = getParentSctId(sctIds, execution);
				for (final Long sctId : sctIdAndParentMap.keySet()) {
					LOGGER.debug("SctId:" + sctId + " parent sctId:" + sctIdAndParentMap.get(sctId));
				}
				LOGGER.info("Start SNOMED ID generation");
				Map<Long,String> sctIdAndSnomedIdMap = new HashMap<>();
				if (!sctIdAndParentMap.isEmpty()) {
					sctIdAndSnomedIdMap = idGenerator.generateSnomedIds(sctIdAndParentMap);
				}
				LOGGER.info("Generated SnomedIds:" + sctIdAndSnomedIdMap.keySet().size());
				for (final String moduleId : moduleIdAndUuidMap.keySet()) {
					String moduleIdSctId = moduleId;
					if (moduleId.contains("-")) {
					final Long mSctId = cachedSctidFactory.getSCTIDFromCache(moduleId);
					if (mSctId == null) {
						LOGGER.warn("No module id sctID found from cache for uuid: " + moduleId);
					} else {
						moduleIdSctId =  mSctId.toString();
					}
					}
					for (final UUID uuid : moduleIdAndUuidMap.get(moduleId)) {
						final Long sctId = cachedSctidFactory.getSCTIDFromCache(uuid.toString());
						writer.write(buildSimpleRefsetMapDeltaLine(uuidGenerator.uuid(), effectiveDate, moduleIdSctId, CTV3_ID_REFSET_ID, sctId, uuidCtv3IdMap.get(uuid)));
						writer.write(RF2Constants.LINE_ENDING);
						writer.write(buildSimpleRefsetMapDeltaLine(uuidGenerator.uuid(), effectiveDate, moduleIdSctId, SNOMED_ID_REFSET_ID, sctId, sctIdAndSnomedIdMap.get(sctId)));
						writer.write(RF2Constants.LINE_ENDING);
					}
				}
			}
		} catch (final IOException e) {
			throw new TransformationException("Error occurred when transforming " + simpleRefsetMapDelta, e);
		} 
	}

	private Map<Long, Long> getParentSctId(final List<Long> sourceSctIds, final Execution execution) throws TransformationException {
		final ParentSctIdFinder finder = new ParentSctIdFinder();
		final String statedRelationsipDelta = STATED_RELATIONSHIP_DELTA_FILE_PREFIX + execution.getBuild().getEffectiveTimeSnomedFormat() + RF2Constants.TXT_FILE_EXTENSION;
		final InputStream transformedDeltaInput = executionDAO.getTransformedFileAsInputStream(execution, execution.getBuild().getBusinessKey(), statedRelationsipDelta);
		final Map<Long, Long> result = finder.getParentSctIdFromStatedRelationship(transformedDeltaInput, sourceSctIds);
		return result;
	}

	private String buildSimpleRefsetMapDeltaLine(final String componentId, final String effectiveDate, final String moduleId, final String refsetId, final Long sctId, final String mapTarget) {
		final StringBuilder builder = new StringBuilder();
		builder.append(componentId);
		builder.append(TAB);
		builder.append(effectiveDate);
		builder.append(TAB);
		builder.append("1");
		builder.append(TAB);
		builder.append(moduleId);
		builder.append(TAB);
		builder.append(refsetId);
		builder.append(TAB);
		builder.append(sctId != null ? sctId.toString() : "null");
		builder.append(TAB);
		builder.append(mapTarget);
		builder.append(TAB);
		return builder.toString();
	}
}
