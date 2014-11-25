package org.ihtsdo.buildcloud.service.execution.transform;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ExecutionPackageReport;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.exception.BadInputFileException;
import org.ihtsdo.buildcloud.service.exception.EffectiveDateNotMatchedException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TransformationService {

	public static final int INTERNATIONAL_NAMESPACE_ID = 0;

	@Autowired
	private IdAssignmentBI idAssignmentBI;

	@Autowired
	private ExecutionDAO dao;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private ModuleResolverService moduleResolverService;

	@Autowired
	private ExecutionDAO executionDAO;

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

	private final ExecutorService executorService;
	@Autowired
	private LegacyIdTransformationService legacyIdTransformation;
	@Autowired
	private Boolean isLegacyIdsRequired;


	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationService.class);

	private static final String SIMPLE_REFSET_MAP_DELTA = "sRefset_SimpleMapDelta_INT";

	public TransformationService() {
		executorService = Executors.newCachedThreadPool();
		
	}

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 * @throws FileNotFoundException 
	 * @throws TransformationException 
	 */
	public void transformFiles(final Execution execution, final Package pkg, final Map<String, TableSchema> inputFileSchemaMap)
			throws ExecutionException, FileNotFoundException, TransformationException {
		final String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveTimeSnomedFormat();
		final String executionId = execution.getId();
		final TransformationFactory transformationFactory = getTransformationFactory(effectiveDateInSnomedFormat, executionId);
		final String packageBusinessKey = pkg.getBusinessKey();
		final boolean workbenchDataFixesRequired = pkg.isWorkbenchDataFixesRequired();
		
		LOGGER.info("Transforming files in execution {}, package {}{}", execution.getId(), packageBusinessKey,
				workbenchDataFixesRequired ? ", workbench data fixes enabled" : "");

		// Iterate each execution input file
		final List<String> executionInputFileNames = dao.listInputFileNames(execution, packageBusinessKey);
		LOGGER.info("Found {} files to process", executionInputFileNames.size());
		if (workbenchDataFixesRequired) {
			// Phase 0
			// Get list of conceptIds which should be in the model module.

			if (!pkg.isFirstTimeRelease()) {
				final String previousPublishedPackage = pkg.getPreviousPublishedPackage();
				try {
					final InputStream statedRelationshipSnapshotStream = executionDAO.getPublishedFileArchiveEntry(pkg.getBuild().getProduct(), "sct2_StatedRelationship_Snapshot", previousPublishedPackage);
					if (statedRelationshipSnapshotStream != null) {
						final Set<String> modelConceptIds = moduleResolverService.getExistingModelConceptIds(statedRelationshipSnapshotStream);

						String inputStatedRelationshipFilename = null;
						for (final String inputFileName : executionInputFileNames) {
							if (inputFileName.startsWith("rel2_StatedRelationship_Delta")) {
								inputStatedRelationshipFilename = inputFileName;
							}
						}
						if (inputStatedRelationshipFilename != null) {
							final InputStream inputStatedRelationshipStream = dao.getInputFileStream(execution, packageBusinessKey, inputStatedRelationshipFilename);
							moduleResolverService.addNewModelConceptIds(modelConceptIds, inputStatedRelationshipStream);

							transformationFactory.setModelConceptIdsForModuleIdFix(modelConceptIds);

						} else {
							// TODO: Add to execution report?
							LOGGER.error("No stated relationship input file found.");
						}
					} else {
						LOGGER.error("No previous stated relationship file found.");
					}
				} catch (BadInputFileException | IOException e) {
					// TODO: Add to execution report?
					LOGGER.error("Exception occurred during moduleId workbench data fix.", e);
				}
			}
		}

		// Phase 1
		// Process just the id and moduleId columns of any Concept and Description files.
		Map<String, List<UUID>> moduleIdAndNewConceptUUids = null;
		boolean isSimpeRefsetMapDeltaPresent = false;
		for (final String filename : executionInputFileNames) {
			if ( filename.contains(SIMPLE_REFSET_MAP_DELTA)) {
				isSimpeRefsetMapDeltaPresent = true;
				break;
			}
		}
		for (final String inputFileName : executionInputFileNames) {
			try {
				LOGGER.info("Processing file: {}", inputFileName);
				final TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);

				if (tableSchema == null) {
					LOGGER.warn("No table schema found in map for file: {}", inputFileName);
				} else {

					checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);

					final ComponentType componentType = tableSchema.getComponentType();
					final ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
					if (isPreProcessType(componentType)) {

						final InputStream executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, inputFileName);
						final OutputStream transformedOutputStream = dao.getLocalTransformedFileOutputStream(execution, packageBusinessKey, inputFileName);

						final StreamingFileTransformation steamingFileTransformation = transformationFactory.getPreProcessFileTransformation(componentType);

						// Apply transformations
						steamingFileTransformation.transformFile(executionInputFileInputStream, transformedOutputStream, inputFileName, report);
						if ( componentType == ComponentType.CONCEPT && isSimpeRefsetMapDeltaPresent && isLegacyIdsRequired.booleanValue()) {
							moduleIdAndNewConceptUUids = getNewConceptUUIDs(execution, packageBusinessKey, inputFileName);
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
		for (final String inputFileName : executionInputFileNames) {
			// Transform all txt files
			final TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);
			if (tableSchema != null) {
				// Recognised RF2 file

				checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);

				final Future<?> future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							InputStream executionInputFileInputStream;
							if (isPreProcessType(tableSchema.getComponentType())) {
								executionInputFileInputStream = dao.getLocalInputFileStream(execution, packageBusinessKey, inputFileName);
							} else {
								executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, inputFileName);
							}

							final AsyncPipedStreamBean asyncPipedStreamBean = dao.getTransformedFileOutputStream(execution, packageBusinessKey, tableSchema.getFilename());
							final OutputStream executionTransformedOutputStream = asyncPipedStreamBean.getOutputStream();

							// Get appropriate transformations for this file.
							final StreamingFileTransformation steamingFileTransformation = transformationFactory.getSteamingFileTransformation(tableSchema);

							// Get the report to output to
							final ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
							// Apply transformations
							steamingFileTransformation.transformFile(executionInputFileInputStream, executionTransformedOutputStream,
									tableSchema.getFilename(), report);

							// Wait for upload of transformed file to finish
							asyncPipedStreamBean.waitForFinish();
						} catch (final FileRecognitionException e) {
							LOGGER.error("Did not recognise input file '{}'.", inputFileName, e);
						} catch (TransformationException | IOException | NoSuchAlgorithmException e) {
							// Catch blocks just log and let the next file get processed.
							LOGGER.error("Exception occurred when transforming file {}", inputFileName, e);
						} catch (ExecutionException | InterruptedException e) {
							LOGGER.error("Exception occurred when uploading transformed file {}", inputFileName, e);
						}
					}
				});
				concurrentTasks.add(future);

			} else {
				// Not recognised as an RF2 file, copy across without transform
				dao.copyInputFileToOutputFile(execution, packageBusinessKey, inputFileName);
			}

			// Wait for all concurrent tasks to finish
			for (final Future concurrentTask : concurrentTasks) {
				try {
					concurrentTask.get();
				} catch (final InterruptedException e) {
					LOGGER.error("Thread interrupted while waiting for future result.", e);
				}
			}
		}
		if (isLegacyIdsRequired.booleanValue() && isSimpeRefsetMapDeltaPresent && moduleIdAndNewConceptUUids != null && !moduleIdAndNewConceptUUids.isEmpty()) {
			legacyIdTransformation.transformLegacyIds(transformationFactory.getCachedSctidFactory(), moduleIdAndNewConceptUUids, execution, packageBusinessKey);
		}
	}
	
	private Map<String,List<UUID>> getNewConceptUUIDs(final Execution execution, final String packageBusinessKey, final String inputFileName) throws IOException {
		final InputStream inputStream = dao.getInputFileStream(execution, packageBusinessKey, inputFileName);
		
		final Map<String,List<UUID>> moduleIdAndUuidMap = new HashMap<>();
		try (BufferedReader conceptDeltaFileReader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line = null;
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

	public void transformInferredRelationshipFile(final Execution execution, final Package pkg, final String inferredRelationshipSnapshotFilename) {
		final TransformationFactory transformationFactory = getTransformationFactory(execution.getBuild().getEffectiveTimeSnomedFormat(), execution.getId());
		final String businessKey = pkg.getBusinessKey();
		try (AsyncPipedStreamBean outputFileOutputStream = dao.getOutputFileOutputStream(execution, businessKey, inferredRelationshipSnapshotFilename)) {
			final StreamingFileTransformation fileTransformation = transformationFactory.getSteamingFileTransformation(new TableSchema(ComponentType.RELATIONSHIP, inferredRelationshipSnapshotFilename));
			final ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
			fileTransformation.transformFile(
					dao.getTransformedFileAsInputStream(execution, businessKey, inferredRelationshipSnapshotFilename),
					outputFileOutputStream.getOutputStream(),
					inferredRelationshipSnapshotFilename,
					report);
		} catch (IOException | TransformationException | FileRecognitionException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to transform inferred relationship file.", e);
		}
	}

	private TransformationFactory getTransformationFactory(final String effectiveDateInSnomedFormat, final String executionId) {
		final CachedSctidFactory cachedSctidFactory = new CachedSctidFactory(INTERNATIONAL_NAMESPACE_ID, effectiveDateInSnomedFormat,
				executionId, idAssignmentBI, idGenMaxTries, idGenRetryDelaySeconds);

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
			throw new EffectiveDateNotMatchedException("Effective date from build:" + effectiveDate + " does not match the date from input file:" + fileName);
		}
	}
}
