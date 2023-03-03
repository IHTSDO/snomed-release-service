package org.ihtsdo.buildcloud.core.service.build.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.BuildReport;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.ReleaseFileGenerationException;
import org.ihtsdo.buildcloud.core.service.identifier.client.IdServiceRestClient;
import org.ihtsdo.buildcloud.core.service.workbenchdatafix.ModuleResolverService;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.exception.BadInputFileException;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EffectiveDateNotMatchedException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TransformationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationService.class);

	private static final String SIMPLE_REFSET_MAP_DELTA = "sRefset_SimpleMapDelta_INT";
	
	private static final String CONCEPT_DELTA = "sct2_Concept_Delta_INT";

	private final ExecutorService executorService;

	@Autowired
	private IdServiceRestClient idRestClient;

	@Autowired
	private BuildDAO dao;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private ModuleResolverService moduleResolverService;

	@Value("${srs.file-processing.transformBufferSize}")
	private Integer transformBufferSize;

	@Value("${cis.maxTries}")
	private Integer idGenMaxTries;

	@Value("${cis.retryDelaySeconds}")
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
		try {
			
			logInIdServiceRestClient();
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
						final InputStream statedRelationshipSnapshotStream = dao.getPublishedFileArchiveEntry(build.getReleaseCenterKey(), "sct2_StatedRelationship_Snapshot", previousPublishedPackage);
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
			// Add legacy ids for new concepts in the simple map file
			List<String> transformedFileNames = getTransformedDeltaFiles(build);
			if (createLegacyIds && getTransformedDeltaFileName(transformedFileNames, SIMPLE_REFSET_MAP_DELTA) != null) {
				Map<String,Collection<Long>> moduleIdAndNewConceptIds = null;
				try {
					//retrieving the transformed concept delta file
					String conceptDeltaFilename = getTransformedDeltaFileName(transformedFileNames, CONCEPT_DELTA);
					if (conceptDeltaFilename != null) {
						moduleIdAndNewConceptIds = getNewConcepIds(build, conceptDeltaFilename);
					}
					if (moduleIdAndNewConceptIds != null && !moduleIdAndNewConceptIds.isEmpty()) {
						legacyIdTransformation.transformLegacyIds( moduleIdAndNewConceptIds, build, idRestClient);
					} else {
						LOGGER.info("No new concepts found and no legacy ids will be generated.");
					}
				} catch (final TransformationException | IOException e) {
					throw new BusinessServiceException("Failed to create legacy identifiers.", e);
				}
			}
		} finally {
			
			logOutIdServiceClient();
		}
	}

	private String getTransformedDeltaFileName( List<String> transformedFileNames, String partOfFileName) {
		for (final String filename : transformedFileNames) {
			if ( filename.contains(partOfFileName)) {
				 return filename;
			}
		}
		return null;
	}
	
	private List<String> getTransformedDeltaFiles(Build build) throws ReleaseFileGenerationException {
		final List<String> transformedFilePaths = dao.listTransformedFilePaths(build);
		final List<String> validFiles = new ArrayList<>();
		if (transformedFilePaths.size() < 1) {
			throw new ReleaseFileGenerationException("Failed to find any transformed files to convert to output delta files.");
		}
		for (final String fileName : transformedFilePaths) {
			if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)
					&& fileName.contains(RF2Constants.DELTA)) {
				validFiles.add(fileName);
			}
		}
		if (validFiles.size() == 0) {
			throw new ReleaseFileGenerationException("Failed to find any files of type *Delta*.txt transformed in build:" + build.getUniqueId());
		}
		return validFiles;
	}

	private void logOutIdServiceClient() {
		try {
			idRestClient.logOut();
		} catch (RestClientException e) {
			LOGGER.warn("Id service rest client failed to log out.", e);
		}
	}

	private void logInIdServiceRestClient() throws BusinessServiceException {
		try {
			idRestClient.logIn();
		} catch (RestClientException e) {
			//Handle retry later
			throw new BusinessServiceException("Id servie rest client failed to log in.",e );
		}
	}
	
	private Map<String, Collection<Long>> getNewConcepIds(final Build build, final String conceptDelta) throws IOException {
		//load previous concept snapshot 
		Collection<Long> conceptsInPreviousSnapshot = new ArrayList<>();
		if (!build.getConfiguration().isFirstTimeRelease()) {
			String conceptSnapshot = conceptDelta.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			if (build.getConfiguration().isBetaRelease()) {
				conceptSnapshot = conceptSnapshot.replaceFirst(RF2Constants.BETA_RELEASE_PREFIX, "");
			}
			try (InputStream prevousSnapshot = dao.getPublishedFileArchiveEntry(build.getReleaseCenterKey(),
					conceptSnapshot, build.getConfiguration().getPreviousPublishedPackage())){
				if (prevousSnapshot == null) {
					throw new IOException("No equivalent file found in the previous published release:" + conceptSnapshot);
				}
				conceptsInPreviousSnapshot = getIdsFromFile(prevousSnapshot);
			}
		}
		Map<String,Collection<Long>> moduleIdAndConceptMap = new HashMap<>();
		try (InputStream inputStream = dao.getTransformedFileAsInputStream(build, conceptDelta);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
				String line;
				boolean firstLine = true;
				while ((line = reader.readLine()) != null) {
					if (firstLine) {
						firstLine = false;
					} else {
						// Split column values
						final String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
						
						Long conceptId = Long.valueOf(columnValues[0]);
						String moduleId = columnValues[3];
						if (!conceptsInPreviousSnapshot.contains(conceptId)) {
							if(moduleIdAndConceptMap.containsKey(moduleId)) {
								moduleIdAndConceptMap.get(moduleId).add(conceptId);
							} else {
								HashSet<Long> conceptSet = new HashSet<>();
								conceptSet.add(conceptId);
								moduleIdAndConceptMap.put(moduleId, conceptSet);
							}
						}
							
					}
				}
			}
			return moduleIdAndConceptMap;
		}
		
	private Collection<Long> getIdsFromFile(InputStream inputStream) throws IOException {
		Set<Long> result = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line;
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
				} else {
					// Split column values
					final String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
					result.add(Long.valueOf(columnValues[0]));
				}
			}
		}
		return result;
	}

	public TransformationFactory getTransformationFactory(Build build) {
		final String effectiveDateInSnomedFormat = build.getConfiguration().getEffectiveTimeSnomedFormat();
		Integer namespaceId = RF2Constants.INTERNATIONAL_NAMESPACE_ID;
		ExtensionConfig extConfig = build.getConfiguration().getExtensionConfig();
		String moduleId = RF2Constants.INTERNATIONAL_CORE_MODULE_ID;
		if (extConfig != null) {
			namespaceId = Integer.valueOf(extConfig.getNamespaceId());
			if (StringUtils.hasLength(extConfig.getDefaultModuleId())) {
				moduleId = extConfig.getDefaultModuleId();
			} else if (!CollectionUtils.isEmpty(extConfig.getModuleIdsSet())) {
				moduleId = extConfig.getModuleIdsSet().iterator().next();
			}
		}
		LOGGER.info("NamespaceId:" + namespaceId +  " module id:" + moduleId);
		final CachedSctidFactory cachedSctidFactory = new CachedSctidFactory(namespaceId, effectiveDateInSnomedFormat, build, dao, idRestClient, idGenMaxTries, idGenRetryDelaySeconds);
		TransformationFactory transformationFactory = new TransformationFactory(namespaceId.toString(),effectiveDateInSnomedFormat, cachedSctidFactory,
				uuidGenerator, moduleId, RF2Constants.INTERNATIONAL_MODEL_COMPONENT_ID, transformBufferSize);
		transformationFactory.setReplaceEffectiveTime(build.getConfiguration().isReplaceExistingEffectiveTime());
		return transformationFactory;
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
