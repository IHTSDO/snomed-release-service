package org.ihtsdo.buildcloud.service.execution.transform;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	private String modelModuleSctid;

	@Autowired
	private Integer idGenMaxTries;

	@Autowired
	private Integer idGenRetryDelaySeconds;

	private ExecutorService executorService;

	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationService.class);

	public TransformationService() {
		executorService = Executors.newCachedThreadPool();
	}

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 */
	public void transformFiles(final Execution execution, final Package pkg, Map<String, TableSchema> inputFileSchemaMap)
			throws ExecutionException {
		String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveTimeSnomedFormat();
		String executionId = execution.getId();

		CachedSctidFactory cachedSctidFactory = new CachedSctidFactory(INTERNATIONAL_NAMESPACE_ID, effectiveDateInSnomedFormat,
				executionId, idAssignmentBI, idGenMaxTries, idGenRetryDelaySeconds);

		final TransformationFactory transformationFactory = new TransformationFactory(effectiveDateInSnomedFormat, cachedSctidFactory,
				uuidGenerator, modelModuleSctid);

		final String packageBusinessKey = pkg.getBusinessKey();
		final boolean workbenchDataFixesRequired = pkg.isWorkbenchDataFixesRequired();
		LOGGER.info("Transforming files in execution {}, package {}{}", execution.getId(), packageBusinessKey,
				workbenchDataFixesRequired ? ", workbench data fixes enabled" : "");

		// Iterate each execution input file
		List<String> executionInputFileNames = dao.listInputFileNames(execution, packageBusinessKey);
		LOGGER.info("Found {} files to process", executionInputFileNames.size());

		if (workbenchDataFixesRequired) {
			// Phase 0
			// Get list of conceptIds which should be in the model module.

			if (!pkg.isFirstTimeRelease()) {
				String previousPublishedPackage = pkg.getPreviousPublishedPackage();
				try {
					InputStream statedRelationshipSnapshotStream = executionDAO.getPublishedFileArchiveEntry(pkg.getBuild().getProduct(), "sct2_StatedRelationship_Snapshot", previousPublishedPackage);
					if (statedRelationshipSnapshotStream != null) {
						Set<String> modelConceptIds = moduleResolverService.getExistingModelConceptIds(statedRelationshipSnapshotStream);

						String inputStatedRelationshipFilename = null;
						for (String inputFileName : executionInputFileNames) {
							if (inputFileName.startsWith("rel2_StatedRelationship_Delta")) {
								inputStatedRelationshipFilename = inputFileName;
							}
						}

						if (inputStatedRelationshipFilename != null) {
							InputStream inputStatedRelationshipStream = dao.getInputFileStream(execution, packageBusinessKey, inputStatedRelationshipFilename);
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
		for (String inputFileName : executionInputFileNames) {
			try {
				LOGGER.info("Processing file: {}", inputFileName);
				TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);

				if (tableSchema == null) {
					LOGGER.warn("No table schema found in map for file: {}", inputFileName);
				} else {

					checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);

					ComponentType componentType = tableSchema.getComponentType();
					if (isPreProcessType(componentType)) {

						InputStream executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, inputFileName);
						OutputStream transformedOutputStream = dao.getLocalTransformedFileOutputStream(execution, packageBusinessKey, inputFileName);

						StreamingFileTransformation steamingFileTransformation = transformationFactory.getPreProcessFileTransformation(componentType);

						// Apply transformations
						ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
						steamingFileTransformation.transformFile(executionInputFileInputStream, transformedOutputStream, inputFileName,
								report);
					}
				}
			} catch (TransformationException | IOException e) {
				// Catch blocks just log and let the next file get processed.
				LOGGER.error("Exception occurred when transforming file {}", inputFileName, e);
			}
		}

		// Phase 2
		// Process all files
		List<Future> concurrentTasks = new ArrayList<>();
		for (final String inputFileName : executionInputFileNames) {
			// Transform all txt files
			final TableSchema tableSchema = inputFileSchemaMap.get(inputFileName);
			if (tableSchema != null) {
				// Recognised RF2 file

				checkFileHasGotMatchingEffectiveDate(inputFileName, effectiveDateInSnomedFormat);

				Future<?> future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							InputStream executionInputFileInputStream;
							if (isPreProcessType(tableSchema.getComponentType())) {
								executionInputFileInputStream = dao.getLocalInputFileStream(execution, packageBusinessKey, inputFileName);
							} else {
								executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, inputFileName);
							}

							AsyncPipedStreamBean asyncPipedStreamBean = dao.getTransformedFileOutputStream(execution, packageBusinessKey, tableSchema.getFilename());
							OutputStream executionTransformedOutputStream = asyncPipedStreamBean.getOutputStream();

							// Get appropriate transformations for this file.
							StreamingFileTransformation steamingFileTransformation = transformationFactory.getSteamingFileTransformation(tableSchema);

							// Get the report to output to
							ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
							// Apply transformations
							steamingFileTransformation.transformFile(executionInputFileInputStream, executionTransformedOutputStream,
									tableSchema.getFilename(), report);

							// Wait for upload of transformed file to finish
							asyncPipedStreamBean.waitForFinish();

						} catch (FileRecognitionException e) {
							LOGGER.error("Did not recognise input file '{}'.", inputFileName, e);
						} catch (TransformationException | IOException e) {
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
			for (Future concurrentTask : concurrentTasks) {
				try {
					concurrentTask.get();
				} catch (InterruptedException e) {
					LOGGER.error("Thread interrupted while waiting for future result.", e);
				}
			}
		}
	}

	private boolean isPreProcessType(ComponentType componentType) {
		return componentType == ComponentType.CONCEPT || componentType == ComponentType.DESCRIPTION;
	}

	/**
	 * @param fileName      input text file name.
	 * @param effectiveDate date in format of "yyyyMMdd"
	 */
	private void checkFileHasGotMatchingEffectiveDate(String fileName, String effectiveDate) {
		String[] segments = fileName.split(RF2Constants.FILE_NAME_SEPARATOR);
		//last segment will be like 20140131.txt
		String dateFromFile = segments[segments.length - 1].substring(0, effectiveDate.length());
		if (!dateFromFile.equals(effectiveDate)) {
			throw new EffectiveDateNotMatchedException("Effective date from build:" + effectiveDate + " does not match the date from input file:" + fileName);
		}
	}
}
