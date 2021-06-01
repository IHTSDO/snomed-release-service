package org.ihtsdo.buildcloud.core.service.build.transform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.identifier.client.SchemeIdType;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LegacyIdTransformationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LegacyIdTransformationService.class);

	private static final String REFSET_SIMPLE_MAP_DELTA_FILE_PREFIX = "der2_sRefset_SimpleMapDelta_INT_";
	private static final String TAB = "\t";
	private static final String CORE_MODULE_ID = "900000000000207008";
	//suppress the generation of SNOMED RT as it is deprecated from the international 20170731 release onwards.
	private static final boolean SUPPRESS_SNOMED_ID = true;

	@Autowired
	private UUIDGenerator uuidGenerator;
	@Autowired
	private BuildDAO buildDAO;
	
	public void transformLegacyIds(final Map<String,Collection<Long>> moduleIdAndNewConceptsMap, final Build build, final IdServiceRestClient idRestClient) throws TransformationException {
		
		List<Long> conceptIds = new ArrayList<>();
		for (String moduelId : moduleIdAndNewConceptsMap.keySet()) {
			conceptIds.addAll(moduleIdAndNewConceptsMap.get(moduelId));
		}
		LOGGER.info("Total new concepts:" + conceptIds.size());
		
		Map<Long,UUID> sctIdAndUuidMap = new HashMap<>();
		try {
			sctIdAndUuidMap = idRestClient.getUuidsForSctIds(conceptIds);
		} catch (RestClientException e) {
			throw new TransformationException("Failed to get uuids for sctids", e);
		}
		
		//Generate CTV3 ID
		LOGGER.info("Start CTV3ID generation");
		Map<UUID, String> uuidCtv3IdMap = new HashMap<>();
		List<UUID> uuids = new ArrayList<>(sctIdAndUuidMap.values());
		try {
			uuidCtv3IdMap = idRestClient.getOrCreateSchemeIds(uuids,SchemeIdType.CTV3ID, build.getUniqueId());
		} catch (RestClientException e) {
			throw new TransformationException("Failed to generate CTV3 IDs", e);
		}
		LOGGER.info("Created ctv3Ids for new concept ids found: " + uuidCtv3IdMap.size());
		//generate snomed id
		Map<UUID, String> uuidAndSnomedIdMap = new HashMap<>();
		if (!SUPPRESS_SNOMED_ID) {
			try {
				uuidAndSnomedIdMap = idRestClient.getOrCreateSchemeIds(uuids, SchemeIdType.SNOMEDID, build.getUniqueId());
			} catch (RestClientException e) {
				throw new TransformationException("Failed to generate Snomed IDs", e);
			}
			LOGGER.info("Generated SnomedIds:" + uuidAndSnomedIdMap.size());
		}
		
		final String effectiveDate = build.getConfiguration().getEffectiveTimeSnomedFormat();
		String fileNamePrefix = build.getConfiguration().isBetaRelease() ? RF2Constants.BETA_RELEASE_PREFIX + REFSET_SIMPLE_MAP_DELTA_FILE_PREFIX  : REFSET_SIMPLE_MAP_DELTA_FILE_PREFIX;
		final String simpleRefsetMapDelta = fileNamePrefix + effectiveDate + RF2Constants.TXT_FILE_EXTENSION;
		final String orignalTransformedDelta = simpleRefsetMapDelta.replace(RF2Constants.TXT_FILE_EXTENSION, ".tmp");
		//can't append to existing file using S3 so need to rename existing transformed file then write again along with additional data.
		buildDAO.renameTransformedFile(build, simpleRefsetMapDelta, orignalTransformedDelta, false);
		try (
				AsyncPipedStreamBean asyncPipedStreamBean = buildDAO.getTransformedFileOutputStream(build, simpleRefsetMapDelta);
				OutputStream outputStream = asyncPipedStreamBean.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, RF2Constants.UTF_8))) {
				final InputStream inputStream = buildDAO.getTransformedFileAsInputStream(build, orignalTransformedDelta);
				if (inputStream == null) {
					LOGGER.warn("No existing transformed file found for " + simpleRefsetMapDelta);
				} else {
						try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
							String line;
							while ((line = reader.readLine()) != null) {
								writer.write(line);
								writer.write(RF2Constants.LINE_ENDING);
							}
						} catch (final IOException e) {
							throw new TransformationException("Error occurred when reading original simple refset map delta transformed file:" + orignalTransformedDelta, e);
						}
				}
				//appending additional legacy data.
				if (!moduleIdAndNewConceptsMap.isEmpty()) {
					for (final String moduleId : moduleIdAndNewConceptsMap.keySet()) {
						for (final Long sctId : moduleIdAndNewConceptsMap.get(moduleId)) {
							UUID uuid = sctIdAndUuidMap.get(sctId);
							//historical reason that the module id is always set to be the core module id since 2011
							writer.write(constructSimpleRefsetMapDeltaLine(uuidGenerator.uuid(), effectiveDate, CORE_MODULE_ID, RF2Constants.CTV3_ID_REFSET_ID, sctId, uuidCtv3IdMap.get(uuid)));
							writer.write(RF2Constants.LINE_ENDING);
							if (!SUPPRESS_SNOMED_ID) {
								final String snomedId = uuidAndSnomedIdMap.get(uuid);
								if (snomedId != null && !snomedId.equals("")) {
									writer.write(constructSimpleRefsetMapDeltaLine(uuidGenerator.uuid(), effectiveDate, CORE_MODULE_ID, RF2Constants.SNOMED_ID_REFSET_ID, sctId, snomedId));
									writer.write(RF2Constants.LINE_ENDING);
								} else {
									LOGGER.warn("No SnomedID was generated for UUID:" + uuid);
								}
							}
						}
					}
				}
		} catch (final IOException e) {
			throw new TransformationException("Error occurred when transforming " + simpleRefsetMapDelta, e);
		}
		
		
	}

	private String constructSimpleRefsetMapDeltaLine(final String componentId, final String effectiveDate, final String moduleId, final String refsetId, final Long sctId, final String mapTarget) {
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
		builder.append(sctId != null ? sctId.toString() : RF2Constants.NULL_STRING);
		builder.append(TAB);
		builder.append(mapTarget);
		return builder.toString();
	}
}
