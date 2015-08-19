package org.ihtsdo.buildcloud.service.build.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.BuildReport;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.otf.rest.exception.BadInputFileException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EffectiveDateNotMatchedException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TransformationService {

	public static final int INTERNATIONAL_NAMESPACE_ID = 0;

	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationService.class);

	private static final String SIMPLE_REFSET_MAP_DELTA = "sRefset_SimpleMapDelta_INT";

	private final ExecutorService executorService;

	@Autowired
	private IdAssignmentBI idAssignmentBI;

	@Autowired
	private BuildDAO dao;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private ModuleResolverService moduleResolverService;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private String coreModuleSctid;

	@Autowired
	private String modelModuleSctid;

	@Autowired
	private Integer transformBufferSize;

	@Autowired
	private Integer idGenMaxTries;

	@Autowired
	private Integer idGenRetryDelaySeconds;

	@Autowired
	private LegacyIdTransformationService legacyIdTransformation;
	
	public TransformationService() {
		executorService = Executors.newCachedThreadPool();
		
	}

	/**
	 * A streaming transformation of build input files, creating build output files.
	 * @throws NoSuchAlgorithmException 
	 */
	public void transformFiles(final Build build, final Map<String, TableSchema> inputFileSchemaMap)
			throws BusinessServiceException, NoSuchAlgorithmException {

		BuildConfiguration configuration = build.getConfiguration();
		final BuildReport report = build.getBuildReport();

		final String effectiveDateInSnomedFormat = configuration.getEffectiveTimeSnomedFormat();
		final TransformationFactory transformationFactory = getTransformationFactory(build);
		final boolean workbenchDataFixesRequired = configuration.isWorkbenchDataFixesRequired();
		final boolean createLegacyIds = configuration.isCreateLegacyIds();
		final boolean isBeta = configuration.isBetaRelease();

		LOGGER.info("Transforming files in build {}, workbench data fixes {}.", build.getUniqueId(), workbenchDataFixesRequired ? "enabled" : "disabled");

		// Iterate each build input file
		final List<String> buildInputFileNames = dao.listInputFileNames(build);
		LOGGER.info("Found {} files to process", buildInputFileNames.size());
		if (workbenchDataFixesRequired) {
			// Phase 0
			// Get list of conceptIds which should be in the model module.

			if (!configuration.isFirstTimeRelease()) {
				final String previousPublishedPackage = configuration.getPreviousPublishedPackage();
				try {
					final InputStream statedRelationshipSnapshotStream = buildDAO.getPublishedFileArchiveEntry(build.getProduct().getReleaseCenter(), "sct2_StatedRelationship_Snapshot", previousPublishedPackage);
					if (statedRelationshipSnapshotStream != null) {
						final Set<String> modelConceptIds = moduleResolverService.getExistingModelConceptIds(statedRelationshipSnapshotStream);

						String inputStatedRelationshipFilename = null;
						for (final String inputFileName : buildInputFileNames) {
							if (inputFileName.startsWith("rel2_StatedRelationship_Delta")) {
								inputStatedRelationshipFilename = inputFileName;
							}
						}
						if (inputStatedRelationshipFilename != null) {
							final InputStream inputStatedRelationshipStream = dao.getInputFileStream(build, inputStatedRelationshipFilename);
							moduleResolverService.addNewModelConceptIds(modelConceptIds, inputStatedRelationshipStream);

							transformationFactory.setModelConceptIdsForModuleIdFix(modelConceptIds);

						} else {
							// TODO: Add to build report?
							LOGGER.error("No stated relationship input file found.");
						}
					} else {
						LOGGER.error("No previous stated relationship file found.");
					}
				} catch (BadInputFileException | IOException e) {
					// TODO: Add to build report?
					LOGGER.error("Exception occurred during moduleId workbench data fix.", e);
				}
			}
		}

		// Phase 1
		// Process just the id and moduleId columns of any Concept and Description files.
		Map<String, List<UUID>> moduleIdAndNewConceptUUids = null;
		boolean isSimpeRefsetMapDeltaPresent = false;
		for (final String filename : buildInputFileNames) {
			if ( filename.contains(SIMPLE_REFSET_MAP_DELTA)) {
				isSimpeRefsetMapDeltaPresent = true;
				break;
			}
		}
		
		for (final String inputFileName : buildInputFileNames) {
			try{
				LOGGER.info("Processing file: {}", inputFileName);
				final TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);
				if (tableSchema == null) {
					LOGGER.warn("No table schema found in map for file: {}", inputFileName);
				} else {
					checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);
					final ComponentType componentType = tableSchema.getComponentType();
					if (isPreProcessType(componentType)) {
						final InputStream buildInputFileInputStream = dao.getInputFileStream(build, inputFileName);
						final OutputStream transformedOutputStream = dao.getLocalTransformedFileOutputStream(build, inputFileName);

						final StreamingFileTransformation steamingFileTransformation = transformationFactory.getPreProcessFileTransformation(componentType);

						// Apply transformations
						steamingFileTransformation.transformFile(buildInputFileInputStream, transformedOutputStream, inputFileName, report);
						if ( componentType == ComponentType.CONCEPT && isSimpeRefsetMapDeltaPresent && createLegacyIds) {
							moduleIdAndNewConceptUUids = getNewConceptUUIDs(build, inputFileName);
						}
					}
				}
			} catch (TransformationException | IOException e) {
				// Catch blocks just log and let the next file get processed.
				LOGGER.error("Exception occurred when transforming file {}", inputFileName, e);
			}
		}
		// Phase 2
		// Process all files
		final List<Future> concurrentTasks = new ArrayList<>();
		for (final String inputFileName : buildInputFileNames) {
			// Transform all txt files
			final TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);
			if (tableSchema != null) {
				// Recognised RF2 file

				checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);
				final String outputFilename = isBeta ? BuildConfiguration.BETA_PREFIX + tableSchema.getFilename() : tableSchema.getFilename();
				final Future<?> future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							InputStream buildInputFileInputStream;
							if (isPreProcessType(tableSchema.getComponentType())) {
								buildInputFileInputStream = dao.getLocalInputFileStream(build, inputFileName);
							} else {
								buildInputFileInputStream = dao.getInputFileStream(build, inputFileName);
							}
							final AsyncPipedStreamBean asyncPipedStreamBean = dao.getTransformedFileOutputStream(build, outputFilename);
							final OutputStream buildTransformedOutputStream = asyncPipedStreamBean.getOutputStream();

							// Get appropriate transformations for this file.
							final StreamingFileTransformation steamingFileTransformation = transformationFactory.getSteamingFileTransformation(tableSchema);

							// Get the report to output to
							// Apply transformations
							steamingFileTransformation.transformFile(buildInputFileInputStream, buildTransformedOutputStream,
									outputFilename, report);

							// Wait for upload of transformed file to finish
							asyncPipedStreamBean.waitForFinish();
						} catch (final FileRecognitionException e) {
							LOGGER.error("Did not recognise input file '{}'.", inputFileName, e);
						} catch (TransformationException | IOException | NoSuchAlgorithmException e) {
							// Catch blocks just log and let the next file get processed.
							LOGGER.error("Exception occurred when transforming file {}", inputFileName, e);
						} catch (ExecutionException | InterruptedException e) {
							dao.renameTransformedFile(build, outputFilename, outputFilename.replace(RF2Constants.TXT_FILE_EXTENSION, ".error"), true);
							LOGGER.error("Exception occurred when uploading transformed file {}", inputFileName, e);
						}
					}
				});
				concurrentTasks.add(future);
			} else {
				// Not recognised as an RF2 file, copy across without transform
				dao.copyInputFileToOutputFile(build, inputFileName);
			}

			// Wait for all concurrent tasks to finish
			for (final Future concurrentTask : concurrentTasks) {
				try {
					concurrentTask.get();
				} catch (ExecutionException | InterruptedException e) {
					LOGGER.error("Thread interrupted while waiting for future result.", e);
				}
			}
		}

		if (createLegacyIds && isSimpeRefsetMapDeltaPresent && moduleIdAndNewConceptUUids != null && !moduleIdAndNewConceptUUids.isEmpty()) {
			try {
				legacyIdTransformation.transformLegacyIds(transformationFactory.getCachedSctidFactory(), moduleIdAndNewConceptUUids, build);
			} catch (final TransformationException e) {
				throw new BusinessServiceException("Failed to create legacy identifiers.", e);
			}
		}
	}
	
	private Map<String,List<UUID>> getNewConceptUUIDs(final Build build, final String inputFileName) throws IOException {
		final InputStream inputStream = dao.getInputFileStream(build, inputFileName);
		
		final Map<String,List<UUID>> moduleIdAndUuidMap = new HashMap<>();
		try (BufferedReader conceptDeltaFileReader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line;
			boolean firstLine = true;

			while ((line = conceptDeltaFileReader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
				} else {
					// Split column values
					final String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
					if (columnValues[0].contains("-")) {
						final String uuidStr = columnValues[0];
						final String moduleId = columnValues[3];
						List<UUID> uuids = moduleIdAndUuidMap.get(moduleId);
						if (uuids == null) {
							uuids = new ArrayList<>();
							moduleIdAndUuidMap.put(moduleId, uuids);
						}
						uuids.add(UUID.fromString(uuidStr));
					}
				}
			}
		}
		return moduleIdAndUuidMap;
	}

	public void transformInferredRelationshipFile(final Build build, final String relationshipFilename,
			Map<String, String> existingUuidToSctidMap) {

		final TransformationFactory transformationFactory = getTransformationFactory(build);
		transformationFactory.setExistingUuidToSctidMap(existingUuidToSctidMap);

		try (AsyncPipedStreamBean outputFileOutputStream = dao.getOutputFileOutputStream(build, relationshipFilename)) {
			final StreamingFileTransformation fileTransformation = transformationFactory.getSteamingFileTransformation(
					new TableSchema(ComponentType.RELATIONSHIP, relationshipFilename));
			final BuildReport report = build.getBuildReport();
			fileTransformation.transformFile(
					dao.getTransformedFileAsInputStream(build, relationshipFilename),
					outputFileOutputStream.getOutputStream(),
					relationshipFilename,
					report);
		} catch (IOException | TransformationException | FileRecognitionException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to transform inferred relationship file.", e);
		}
	}

	public TransformationFactory getTransformationFactory(Build build) {
		final String effectiveDateInSnomedFormat = build.getConfiguration().getEffectiveTimeSnomedFormat();
		final String buildId =  build.getId();
		final CachedSctidFactory cachedSctidFactory = new CachedSctidFactory(INTERNATIONAL_NAMESPACE_ID, effectiveDateInSnomedFormat,
				buildId, idAssignmentBI, idGenMaxTries, idGenRetryDelaySeconds);

		return new TransformationFactory(effectiveDateInSnomedFormat, cachedSctidFactory,
				uuidGenerator, coreModuleSctid, modelModuleSctid, transformBufferSize);
	}
	
	private boolean isPreProcessType(final ComponentType componentType) {
		return componentType == ComponentType.CONCEPT || componentType == ComponentType.DESCRIPTION;
	}

	/**
	 * @param fileName      input text file name.
	 * @param effectiveDate date in format of "yyyyMMdd"
	 */
	private void checkFileHasGotMatchingEffectiveDate(final String fileName, final String effectiveDate) {
		final String[] segments = fileName.split(RF2Constants.FILE_NAME_SEPARATOR);
		//last segment will be like 20140131.txt
		final String dateFromFile = segments[segments.length - 1].substring(0, effectiveDate.length());
		if (!dateFromFile.equals(effectiveDate)) {
			throw new EffectiveDateNotMatchedException("Effective date from product:" + effectiveDate + " does not match the date from input file:" + fileName);
		}
	}
}
