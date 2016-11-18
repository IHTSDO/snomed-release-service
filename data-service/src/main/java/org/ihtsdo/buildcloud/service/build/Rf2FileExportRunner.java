package org.ihtsdo.buildcloud.service.build;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.build.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.build.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.build.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.build.database.Rf2FileWriter;
import org.ihtsdo.buildcloud.service.build.database.map.RF2TableDAOTreeMapImpl;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.helper.StatTimer;
import org.ihtsdo.otf.rest.exception.BadConfigurationException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;

public class Rf2FileExportRunner {

	private static final String HYPHEN = "-";
	private final Build build;
	private final ReleaseCenter releaseCenter;
	private final BuildDAO buildDao;
	private final int maxRetries;
	private final UUIDGenerator uuidGenerator;
	private static final Logger LOGGER = LoggerFactory.getLogger(Rf2FileExportRunner.class);
	private final BuildConfiguration configuration;

	public Rf2FileExportRunner(final Build build, final BuildDAO dao, final UUIDGenerator uuidGenerator, final int maxRetries) {
		this.build = build;
		configuration = build.getConfiguration();
		releaseCenter = build.getProduct().getReleaseCenter();
		buildDao = dao;
		this.maxRetries = maxRetries;
		this.uuidGenerator = uuidGenerator;
	}

	public final void generateReleaseFiles() throws ReleaseFileGenerationException {
		final List<String> transformedFiles = getTransformedDeltaFiles();
		final BuildConfiguration configuration = build.getConfiguration();
		final Set<String> newRF2InputFiles = configuration.getNewRF2InputFileSet();
		for ( String thisFile : transformedFiles) {
			if (!thisFile.endsWith(TXT_FILE_EXTENSION) || thisFile.startsWith(RELASHIONSHIP_DELTA_PREFIX)) {
				continue;
			}
			int failureCount = 0;
			boolean success = false;
			while (!success) {
				try {
					boolean fileFirstTimeRelease = false;
					String cleanFileName  = thisFile;
					if (configuration.isBetaRelease()) {
						cleanFileName = thisFile.substring(1);
					}
					final boolean newFile = newRF2InputFiles.contains(cleanFileName.replace(SCT2, INPUT_FILE_PREFIX).replace(DER2, INPUT_FILE_PREFIX));
					fileFirstTimeRelease = newFile || configuration.isFirstTimeRelease();
					generateReleaseFile(thisFile, configuration.getCustomRefsetCompositeKeys(), fileFirstTimeRelease);
					success = true;
				} catch (final Exception e) {
					failureCount = handleException(e, thisFile, failureCount);
				}
			}
		}
	}

	public void generateRelationshipFilesFromTransformedClassifierResult(final String transformedSnapshotFilename) throws ReleaseFileGenerationException {
		boolean success = false;
		int failureCount = 0;
		while (!success) {
			try {
				generateInferredRelationshipFiles(transformedSnapshotFilename);
				success = true;
			} catch (final Exception e) {
				failureCount = handleException(e, transformedSnapshotFilename, failureCount);
			}
		}
	}

	private void generateReleaseFile(final String transformedDeltaDataFile, final Map<String, List<Integer>> customRefsetCompositeKeys,
			final boolean fileFirstTimeRelease) throws ReleaseFileGenerationException {

		final String effectiveTime = configuration.getEffectiveTimeSnomedFormat();
		final String previousPublishedPackage = configuration.getPreviousPublishedPackage();

		LOGGER.info("Generating release file using {}, isFirstRelease={}", transformedDeltaDataFile, fileFirstTimeRelease);
		final StatTimer timer = new StatTimer(getClass());
		RF2TableDAO rf2TableDAO = null;
		TableSchema tableSchema = null;

		try {
			// Create table containing transformed input delta
			LOGGER.debug("Start: creating table for {}", transformedDeltaDataFile);
			final InputStream transformedDeltaInputStream = buildDao.getTransformedFileAsInputStream(build,
					transformedDeltaDataFile);

			rf2TableDAO = new RF2TableDAOTreeMapImpl(customRefsetCompositeKeys);
			timer.split();
			final boolean workbenchDataFixesRequired = configuration.isWorkbenchDataFixesRequired();
			tableSchema = rf2TableDAO.createTable(transformedDeltaDataFile, transformedDeltaInputStream, workbenchDataFixesRequired);
			//Replace Delta_ to Snapshot_ add file name separator due to some extension refsets have the word "delta" as part of the refset name

			final String currentSnapshotFileName = constructFullOrSnapshotFilename(transformedDeltaDataFile, SNAPSHOT);
			String cleanCurrentSnapshotFileName = currentSnapshotFileName;
			if (configuration.isBetaRelease() && currentSnapshotFileName.startsWith(BuildConfiguration.BETA_PREFIX)) {
				cleanCurrentSnapshotFileName = currentSnapshotFileName.substring(1); // Previous file will not be a beta release
			}

			if (workbenchDataFixesRequired) {
				if (!fileFirstTimeRelease) {
					if (tableSchema.getComponentType() == ComponentType.REFSET) {
						// Workbench workaround - correct refset member ids using previous snapshot file.
						// See interface javadoc for more info.
						rf2TableDAO.reconcileRefsetMemberIds(getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName),
								currentSnapshotFileName, effectiveTime);
					}

					//Workbench workaround for dealing Attribute Value File with empty valueId
					//ideally we should combine all workbench workaround together so that don't read snapshot file twice
					if (transformedDeltaDataFile.contains(ATTRIBUTE_VALUE_FILE_IDENTIFIER)) {
						rf2TableDAO.resolveEmptyValueId(getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName),
								effectiveTime);
					}

					// Workbench workaround - use full file to discard invalid delta entries
					// See interface javadoc for more info.
					rf2TableDAO.discardAlreadyPublishedDeltaStates(
							getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName), currentSnapshotFileName,
							effectiveTime);
				}
			}

			LOGGER.debug("Start: Exporting delta file for {}", tableSchema.getTableName());
			timer.setTargetEntity(tableSchema.getTableName());
			timer.logTimeTaken("Create table");

			// Export ordered Delta file
			final Rf2FileWriter rf2FileWriter = new Rf2FileWriter();
			final AsyncPipedStreamBean deltaFileAsyncPipe = buildDao
					.getOutputFileOutputStream(build, transformedDeltaDataFile);

			RF2TableResults deltaResultSet;
			timer.split();
			if (fileFirstTimeRelease) {
				deltaResultSet = rf2TableDAO.selectNone(tableSchema);
				timer.logTimeTaken("Select none");
			} else {
				deltaResultSet = rf2TableDAO.selectAllOrdered(tableSchema);
				timer.logTimeTaken("Select all ordered");
			}

			timer.split();
			rf2FileWriter.exportDelta(deltaResultSet, tableSchema, deltaFileAsyncPipe.getOutputStream());
			LOGGER.debug("Completed processing delta file for {}, waiting for network", tableSchema.getTableName());
			timer.logTimeTaken("Export delta processing");
			deltaFileAsyncPipe.waitForFinish();
			LOGGER.debug("Finish: Exporting delta file for {}", tableSchema.getTableName());

			final String currentFullFileName = constructFullOrSnapshotFilename(transformedDeltaDataFile, FULL);
			if (!fileFirstTimeRelease) {
				String cleanFullFileName = currentFullFileName;
				if (configuration.isBetaRelease() && currentFullFileName.startsWith(BuildConfiguration.BETA_PREFIX)) {
					cleanFullFileName = currentFullFileName.substring(1); // Previous file will not be a beta release
				}
				final InputStream previousFullFileStream = getPreviousFileStream(previousPublishedPackage, cleanFullFileName);

				// Append transformed previous full file
				LOGGER.debug("Start: Insert previous release data into table {}", tableSchema.getTableName());
				timer.split();
				rf2TableDAO.appendData(tableSchema, previousFullFileStream, workbenchDataFixesRequired);
				timer.logTimeTaken("Insert previous release data");
				LOGGER.debug("Finish: Insert previous release data into table {}", tableSchema.getTableName());
			}

			// Export Full and Snapshot files
			final AsyncPipedStreamBean fullFileAsyncPipe = buildDao
					.getOutputFileOutputStream(build, currentFullFileName);
			final String snapshotOutputFilePath = constructFullOrSnapshotFilename(transformedDeltaDataFile, SNAPSHOT);
			final AsyncPipedStreamBean snapshotAsyncPipe = buildDao
					.getOutputFileOutputStream(build, snapshotOutputFilePath);

			timer.split();
			final RF2TableResults fullResultSet = rf2TableDAO.selectAllOrdered(tableSchema);
			timer.logTimeTaken("selectAllOrdered");

			rf2FileWriter.exportFullAndSnapshot(fullResultSet, tableSchema,
					build.getConfiguration().getEffectiveTime(), fullFileAsyncPipe.getOutputStream(),
					snapshotAsyncPipe.getOutputStream());
			LOGGER.debug("Completed processing full and snapshot files for {}, waiting for network.", tableSchema.getTableName());
			fullFileAsyncPipe.waitForFinish();
			snapshotAsyncPipe.waitForFinish();
		} catch (final Exception e) {
			final String errorMsg = "Failed to generate subsequent full and snapshort release files due to: " + e.getMessage();
			throw new ReleaseFileGenerationException(errorMsg, e);
		} finally {
			// Clean up time
			if (rf2TableDAO != null) {
				try {
					rf2TableDAO.closeConnection();
				} catch (final Exception e) {
					LOGGER.error("Failure while trying to clean up after {}",
							tableSchema != null ? tableSchema.getTableName() : "No table yet.", e);
				}
			}
		}
	}

	private void generateInferredRelationshipFiles(final String snapshotOutputFilename) throws ReleaseFileGenerationException {

		final String deltaFilename = snapshotOutputFilename.replace(SNAPSHOT, DELTA);
		final String fullFilename = snapshotOutputFilename.replace(SNAPSHOT, FULL);

		// Import snapshot
		final InputStream snapshotInputStream = buildDao.getTransformedFileAsInputStream(build, snapshotOutputFilename);
		RF2TableDAOTreeMapImpl rf2TableDAO = new RF2TableDAOTreeMapImpl(null);
		TableSchema tableSchema = null;
		try {
			tableSchema = rf2TableDAO.createTable(snapshotOutputFilename, snapshotInputStream, false);
			// additional relationship fix 
			//add existing additional relationships from the input inferred delta if there is any to the new delta
			InputStream additionalRelationshipInputStream = buildDao.getTransformedFileAsInputStream(build, deltaFilename);
			if (additionalRelationshipInputStream != null) {
				LOGGER.info("Appending additional relationships to relationship delta file for build id:" + build.getId() );
				rf2TableDAO.appendData(tableSchema, additionalRelationshipInputStream, false);
			}
		} catch (IOException | FileRecognitionException | DatabasePopulatorException | NumberFormatException | BadConfigurationException e) {
			throw new ReleaseFileGenerationException("Failed to create table from " + snapshotOutputFilename, e);
		}

		// Export delta
		final Rf2FileWriter rf2FileWriter = new Rf2FileWriter();
		RF2TableResults results = null;
		results = rf2TableDAO.selectWithEffectiveDateOrdered(tableSchema, configuration.getEffectiveTimeSnomedFormat());
		try (AsyncPipedStreamBean deltaOutputStream = buildDao.getOutputFileOutputStream(build, deltaFilename)) {
			rf2FileWriter.exportDelta(results, tableSchema, deltaOutputStream.getOutputStream());
		} catch (IOException | SQLException e) {
			throw new ReleaseFileGenerationException("Failed to export " + deltaFilename, e);
		} 
		
		// import Delta to generate snapshot and full
		final InputStream deltaInputStream = buildDao.getOutputFileInputStream(build, deltaFilename);
		try {
			rf2TableDAO = new RF2TableDAOTreeMapImpl(null);
			tableSchema =  rf2TableDAO.createTable(deltaFilename, deltaInputStream, false);
		} catch (BadConfigurationException | IOException| FileRecognitionException | DatabasePopulatorException e) {
			throw new ReleaseFileGenerationException("Failed to create table from " + deltaFilename, e);
		}
		// Import any previous full
		if (!configuration.isFirstTimeRelease()) {
			try {
				String cleanFileName = fullFilename;
				if (configuration.isBetaRelease() && fullFilename.startsWith(BuildConfiguration.BETA_PREFIX)) {
					cleanFileName = fullFilename.substring(1); // Previous file will not be a beta release
				}
				final InputStream previousFullStream = getPreviousFileStream(configuration.getPreviousPublishedPackage(), cleanFileName);
				rf2TableDAO.appendData(tableSchema, previousFullStream, false);
			} catch (IOException | DatabasePopulatorException | BadConfigurationException e) {
				throw new ReleaseFileGenerationException("Failed to import previous full " + fullFilename, e);
			}
		}

		// Export snapshot and full
		results = rf2TableDAO.selectAllOrdered(tableSchema);
		try (AsyncPipedStreamBean fullFileAsyncPipe = buildDao.getOutputFileOutputStream(build, fullFilename);
			AsyncPipedStreamBean snapshotFileAsyncPipe = buildDao.getOutputFileOutputStream(build, snapshotOutputFilename)) {
			rf2FileWriter.exportFullAndSnapshot(results, tableSchema,
					build.getConfiguration().getEffectiveTime(), fullFileAsyncPipe.getOutputStream(),
					snapshotFileAsyncPipe.getOutputStream());
		} catch (IOException | SQLException e) {
			throw new ReleaseFileGenerationException("Failed to export " + fullFilename + " and " + snapshotOutputFilename, e);
		}
		//overwrites delta when it is first time release.
		if (configuration.isFirstTimeRelease()) {
			results = rf2TableDAO.selectNone(tableSchema);
			try (AsyncPipedStreamBean deltaOutputStream = buildDao.getOutputFileOutputStream(build, deltaFilename)) {
				rf2FileWriter.exportDelta(results, tableSchema, deltaOutputStream.getOutputStream());
			} catch (IOException | SQLException e) {
				throw new ReleaseFileGenerationException("Failed to export " + deltaFilename, e);
			} 
		} 
	}

	private InputStream getPreviousFileStream(final String previousPublishedPackage, final String currentFileName) throws IOException {
		final InputStream previousFileStream = buildDao.getPublishedFileArchiveEntry(releaseCenter, currentFileName, previousPublishedPackage);
		if (previousFileStream == null) {
			throw new FileNotFoundException("No equivalent of:  "
					+ currentFileName
					+ " found in previous published package:" + previousPublishedPackage);
		}
		return previousFileStream;
	}

	private int handleException(final Exception e, final String thisFile, int failureCount) throws ReleaseFileGenerationException {
		// Is this an error that it's worth retrying eg root cause IOException or AWS Related?
		final Throwable cause = e.getCause();
		failureCount++;
		if (failureCount > maxRetries) {
			throw new ReleaseFileGenerationException("Maximum failure recount of " + maxRetries + " exceeeded. Last error: "
					+ e.getMessage(), e);
		} else if (isNetworkRelated(cause)) {
			LOGGER.warn("Failure while processing {} due to: {}. Retrying ({})...", thisFile, e.getMessage(), failureCount);
		} else {
			// If this isn't something we think we might recover from by retrying, then just re-throw the existing error
			throw new ReleaseFileGenerationException("Failed to generate release file:" + thisFile, e);
		}
		return failureCount;
	}

	private boolean isNetworkRelated(final Throwable cause) {
		boolean isNetworkRelated = false;
		if (cause != null &&
				(cause instanceof IOException || cause instanceof AmazonServiceException || cause instanceof AmazonClientException)) {
			isNetworkRelated = true;
		}
		return isNetworkRelated;
	}

	/**
	 * @return the transformed delta file name exception if not found.
	 * @throws ReleaseFileGenerationException
	 */
	private List<String> getTransformedDeltaFiles() throws ReleaseFileGenerationException {
		final List<String> transformedFilePaths = buildDao.listTransformedFilePaths(build);
		final List<String> validFiles = new ArrayList<>();
		if (transformedFilePaths.size() < 1) {
			throw new ReleaseFileGenerationException("Failed to find any transformed files to convert to output delta files.");
		}

		for (final String fileName : transformedFilePaths) {
			if (fileName.endsWith(TXT_FILE_EXTENSION)
					&& fileName.contains(DELTA)) {
				validFiles.add(fileName);
			}
		}
		if (validFiles.size() == 0) {
			throw new ReleaseFileGenerationException(
					"Failed to find any files of type *Delta*.txt transformed in build:" + build.getUniqueId());
		}
		return validFiles;
	}
	
	/** To handle when extension refset name has got "delta" as part of the file name. 
	 * e.g rel2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetDelta_SE1000052_20161130.txt
	 * @param deltaFilename
	 * @param fullOrSnapshot
	 * @return
	 */
	private String constructFullOrSnapshotFilename(String deltaFilename, String fullOrSnapshot) {
		return deltaFilename.replace(DELTA + FILE_NAME_SEPARATOR, fullOrSnapshot + FILE_NAME_SEPARATOR).replace(DELTA + HYPHEN, fullOrSnapshot + HYPHEN);
	}
}
