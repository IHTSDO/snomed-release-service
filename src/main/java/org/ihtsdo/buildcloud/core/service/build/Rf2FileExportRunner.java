package org.ihtsdo.buildcloud.core.service.build;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableExportDAO;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableResults;
import org.ihtsdo.buildcloud.core.service.build.database.Rf2FileWriter;
import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.buildcloud.core.service.helper.StatTimer;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;

import static org.ihtsdo.buildcloud.core.entity.BuildConfiguration.BETA_PREFIX;
import static org.ihtsdo.buildcloud.core.service.build.RF2Constants.*;

public class Rf2FileExportRunner {

	private static final String HYPHEN = "-";
	private final Build build;
	private final BuildDAO buildDao;
	private final int maxRetries;
	private static final Logger LOGGER = LoggerFactory.getLogger(Rf2FileExportRunner.class);
	private final BuildConfiguration configuration;

	public Rf2FileExportRunner(final Build build, final BuildDAO dao, final int maxRetries) {
		this.build = build;
		configuration = build.getConfiguration();
		buildDao = dao;
		this.maxRetries = maxRetries;
	}

	public final void generateReleaseFiles() throws ReleaseFileGenerationException {
		final List<String> transformedFiles = getTransformedDeltaFiles();
		final Set<String> newRF2InputFiles = configuration.getNewRF2InputFileSet();
		final Map<String,Set<String>> includedFilesMap = configuration.getIncludedFilesInNewFilesMap();
		for ( String thisFile : transformedFiles) {
			if (!thisFile.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
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
					final boolean newFile = newRF2InputFiles.contains(cleanFileName.replace(RF2Constants.SCT2, RF2Constants.INPUT_FILE_PREFIX).replace(RF2Constants.DER2, RF2Constants.INPUT_FILE_PREFIX));
					fileFirstTimeRelease = newFile || configuration.isFirstTimeRelease();
					Set<String> includedFilesInNewFile = includedFilesMap.get(cleanFileName.replace(RF2Constants.SCT2, RF2Constants.INPUT_FILE_PREFIX).replace(RF2Constants.DER2, RF2Constants.INPUT_FILE_PREFIX));
					generateReleaseFile(thisFile, configuration.getCustomRefsetCompositeKeys(), fileFirstTimeRelease, includedFilesInNewFile);
					success = true;
				} catch (final Exception e) {
					failureCount = handleException(e, thisFile, failureCount);
				}
			}
		}
	}

	public boolean isInferredRelationshipFileExist(final List<String> rf2DeltaFilesSpecifiedByManifest) throws ReleaseFileGenerationException{
		boolean isRelationshipDeltaFileSpecifiedInManifest = false;
		for (String thisFile : rf2DeltaFilesSpecifiedByManifest) {
			if (thisFile.endsWith(RF2Constants.TXT_FILE_EXTENSION) && thisFile.startsWith(RF2Constants.RELASHIONSHIP_DELTA_PREFIX)) {
				isRelationshipDeltaFileSpecifiedInManifest = true;
				break;
			}
		}
		// if the relationship delta is not required in manifest, then skip this check
		if (!isRelationshipDeltaFileSpecifiedInManifest) {
			return true;
		}

		final List<String> transformedFiles = getTransformedDeltaFiles();
		for ( String thisFile : transformedFiles) {
			if (thisFile.endsWith(RF2Constants.TXT_FILE_EXTENSION) && thisFile.startsWith(RF2Constants.RELASHIONSHIP_DELTA_PREFIX)) {
				return  true;
			}
		}
		return  false;
	}

	private void generateReleaseFile(final String transformedDeltaDataFile, final Map<String, List<Integer>> customRefsetCompositeKeys,
			final boolean fileFirstTimeRelease, final Set<String> includedFilesInNewFile) throws ReleaseFileGenerationException {

		final String effectiveTime = configuration.getEffectiveTimeSnomedFormat();
		final String previousPublishedPackage = configuration.getPreviousPublishedPackage();

		LOGGER.info("Generating release file using {}, isFirstRelease={}", transformedDeltaDataFile, fileFirstTimeRelease);
		final StatTimer timer = new StatTimer(getClass());
		RF2TableExportDAO rf2TableDAO = null;
		TableSchema tableSchema = null;
		try {
			// Create table containing transformed input delta
			LOGGER.debug("Start: creating table for {}", transformedDeltaDataFile);
			final InputStream transformedDeltaInputStream = buildDao.getTransformedFileAsInputStream(build, transformedDeltaDataFile);

			rf2TableDAO = new RF2TableExportDAOImpl(customRefsetCompositeKeys);
			timer.split();
			final boolean workbenchDataFixesRequired = configuration.isWorkbenchDataFixesRequired();
			tableSchema = rf2TableDAO.createTable(transformedDeltaDataFile, transformedDeltaInputStream, workbenchDataFixesRequired);

			if (configuration.getExtensionConfig() != null && configuration.getExtensionConfig().isReleaseAsAnEdition()) {
				// Add the international delta for extension edition release.
				Date previousDependencyEffectiveDate = configuration.getExtensionConfig().getPreviousEditionDependencyEffectiveDate();
				// in yyyyMMdd format
				String previousDependencyEffectiveDateStr = RF2Constants.DATE_FORMAT.format(previousDependencyEffectiveDate);
				LOGGER.info("Previous edition dependency effective date is re-formatted to {} ", previousDependencyEffectiveDateStr);
				if (StringUtils.isBlank(configuration.getExtensionConfig().getDependencyRelease())) {
					String msg = "No dependency release package is specified but it is required for edition builds";
					LOGGER.error(msg);
					throw new ReleaseFileGenerationException(msg);
				}
				InputStream intDeltaInputStream = getEquivalentInternationalDelta(configuration.getExtensionConfig(), transformedDeltaDataFile);
				if (intDeltaInputStream != null) {
					rf2TableDAO.appendData(tableSchema, intDeltaInputStream, workbenchDataFixesRequired);
				} else {
					if (previousDependencyEffectiveDate == null) {
						String msg = "No previous dependency effective date specified to generate delta for edition release.";
						LOGGER.error(msg);
						throw new ReleaseFileGenerationException(msg);
					}
					Instant start = Instant.now();
					// Filter delta from dependency full
					InputStream intFullStream = getEquivalentInternationalFull(configuration.getExtensionConfig(), transformedDeltaDataFile);
					if (intFullStream != null) {
						rf2TableDAO.appendData(tableSchema, intFullStream, previousDependencyEffectiveDateStr);
					} else {
						//  Refset files specific to extensions will not have equivalent files in the international release.
						LOGGER.info("No equivalent full file found in dependency package for {}", transformedDeltaDataFile);
					}
					Instant end = Instant.now();
					LOGGER.info("Taken {} seconds to filter delta from international full file to add to {}", Duration.between(start, end).toSeconds(), transformedDeltaDataFile);
				}
			}

			final String currentSnapshotFileName = constructFullOrSnapshotFilename(transformedDeltaDataFile, SNAPSHOT);
			String cleanCurrentSnapshotFileName = currentSnapshotFileName;
			if (configuration.isBetaRelease() && currentSnapshotFileName.startsWith(BETA_PREFIX)) {
				cleanCurrentSnapshotFileName = currentSnapshotFileName.substring(1); // Previous file will not be a beta release
			}

			if (workbenchDataFixesRequired && !fileFirstTimeRelease) {
				if (tableSchema.getComponentType() == ComponentType.REFSET) {
					// Workbench workaround - correct refset member ids using previous snapshot file.
					// See interface javadoc for more info.
					rf2TableDAO.reconcileRefsetMemberIds(getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName),
							currentSnapshotFileName, effectiveTime);
				}

				// Workbench workaround for dealing Attribute Value File with empty valueId
				// ideally we should combine all workbench workaround together so that don't read snapshot file twice
				if (transformedDeltaDataFile.contains(RF2Constants.ATTRIBUTE_VALUE_FILE_IDENTIFIER)) {
					rf2TableDAO.resolveEmptyValueId(getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName),
							effectiveTime);
				}

				// Workbench workaround - use full file to discard invalid delta entries
				// See interface javadoc for more info.
				rf2TableDAO.discardAlreadyPublishedDeltaStates(getPreviousFileStream(previousPublishedPackage, cleanCurrentSnapshotFileName),
						currentSnapshotFileName, effectiveTime);
			}

			LOGGER.debug("Start: Exporting delta file for {}", tableSchema.getTableName());
			timer.setTargetEntity(tableSchema.getTableName());
			timer.logTimeTaken("Create table");

			// Export ordered Delta file
			final Rf2FileWriter rf2FileWriter = new Rf2FileWriter(configuration.getExcludeRefsetDescriptorMembers(), configuration.getExcludeLanguageRefsetIds());
			final AsyncPipedStreamBean deltaFileAsyncPipe = buildDao
					.getOutputFileOutputStream(build, transformedDeltaDataFile);
			
			timer.split();
			RF2TableResults deltaResultSet = rf2TableDAO.selectAllOrdered(tableSchema);
			timer.logTimeTaken("Select all ordered");
			timer.split();
			rf2FileWriter.exportDelta(deltaResultSet, tableSchema, deltaFileAsyncPipe.getOutputStream());
			LOGGER.debug("Completed processing delta file for {}, waiting for network", tableSchema.getTableName());
			timer.logTimeTaken("Export delta processing");
			deltaFileAsyncPipe.waitForFinish();
			LOGGER.debug("Finish: Exporting delta file for {}", tableSchema.getTableName());

			final String currentFullFileName = constructFullOrSnapshotFilename(transformedDeltaDataFile, RF2Constants.FULL);
			if (!fileFirstTimeRelease) {
				String cleanFullFileName = currentFullFileName;
				if (configuration.isBetaRelease() && currentFullFileName.startsWith(BETA_PREFIX)) {
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

			
			if (includedFilesInNewFile != null && !includedFilesInNewFile.isEmpty()) {
				for (String includedFile : includedFilesInNewFile) {
					final String includedFileFullName = constructFullOrSnapshotFilename(includedFile, RF2Constants.FULL);
					final InputStream previousCombinedFullFileStream = getPreviousFileStream(previousPublishedPackage, includedFileFullName);

					// Append transformed previous full file
					LOGGER.debug("Start: Insert previous release data from {} into table {}", includedFileFullName, tableSchema.getTableName());
					timer.split();
					rf2TableDAO.appendData(tableSchema, previousCombinedFullFileStream, workbenchDataFixesRequired);
					timer.logTimeTaken("Insert previous release data");
					LOGGER.debug("Finish: Insert previous release data from {} into table {}", includedFileFullName, tableSchema.getTableName());
				}
			}

			// Export Full and Snapshot files
			final AsyncPipedStreamBean fullFileAsyncPipe = buildDao.getOutputFileOutputStream(build, currentFullFileName);
			final String snapshotOutputFilePath = constructFullOrSnapshotFilename(transformedDeltaDataFile, SNAPSHOT);
			final AsyncPipedStreamBean snapshotAsyncPipe = buildDao.getOutputFileOutputStream(build, snapshotOutputFilePath);

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
			final String errorMsg = "Failed to generate subsequent full and snapshot release files due to: " + ExceptionUtils.getRootCauseMessage(e);
			throw new ReleaseFileGenerationException(errorMsg, e);
		} finally {
			// Clean up time
			if (rf2TableDAO != null) {
				try {
					rf2TableDAO.closeConnection();
				} catch (final Exception e) {
					LOGGER.error("Failure while trying to clean up after {}", tableSchema != null ? tableSchema.getTableName() : "No table yet.", e);
				}
			}
		}
	}

	private InputStream getEquivalentInternationalFull(ExtensionConfig extensionConfig, String transformedDeltaDataFile) throws IOException {
		String equivalentFullFile = getEquivalentInternationalFile(extensionConfig, transformedDeltaDataFile).replace(DELTA, FULL);
		LOGGER.info("Equivalent full file {}", equivalentFullFile);
		return buildDao.getPublishedFileArchiveEntry(INT_RELEASE_CENTER.getBusinessKey(), equivalentFullFile, extensionConfig.getDependencyRelease());
	}

	private InputStream getEquivalentInternationalDelta(ExtensionConfig extensionConfig, String transformedDeltaDataFile) throws IOException {
		return buildDao.getPublishedFileArchiveEntry(INT_RELEASE_CENTER.getBusinessKey(), getEquivalentInternationalFile(extensionConfig, transformedDeltaDataFile), extensionConfig.getDependencyRelease());
	}

	private String getEquivalentInternationalFile(ExtensionConfig extensionConfig, String transformedDeltaDataFile) {
		if (configuration.isBetaRelease() && transformedDeltaDataFile.startsWith(BETA_PREFIX)) {
			transformedDeltaDataFile = transformedDeltaDataFile.substring(1);
		}
		String[] splits = transformedDeltaDataFile.split(RF2Constants.FILE_NAME_SEPARATOR);
		StringBuilder equivalentBuilder = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			equivalentBuilder.append(splits[i]);
			equivalentBuilder.append(RF2Constants.FILE_NAME_SEPARATOR);
		}
		equivalentBuilder.append(RF2Constants.INT);
		equivalentBuilder.append(RF2Constants.FILE_NAME_SEPARATOR);
		equivalentBuilder.append(RF2BuildUtils.getReleaseDateFromReleasePackage(extensionConfig.getDependencyRelease()));
		equivalentBuilder.append(RF2Constants.TXT_FILE_EXTENSION);
		LOGGER.info("The equivalent file for {} in international package is {}", transformedDeltaDataFile, equivalentBuilder);
		return equivalentBuilder.toString();
	}

	private InputStream getPreviousFileStream(final String previousPublishedPackage, final String currentFileName) throws IOException {
		final InputStream previousFileStream = buildDao.getPublishedFileArchiveEntry(build.getReleaseCenterKey(), currentFileName, previousPublishedPackage);
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
		if (cause instanceof IOException || cause instanceof AmazonClientException) {
			isNetworkRelated = true;
		}
		return isNetworkRelated;
	}

	/**
	 * @return the transformed delta file name exception if not found.
	 * @throws ReleaseFileGenerationException ReleaseFileGenerationException
	 */
	private List<String> getTransformedDeltaFiles() throws ReleaseFileGenerationException {
		final List<String> transformedFilePaths = buildDao.listTransformedFilePaths(build);
		final List<String> validFiles = new ArrayList<>();
		if (transformedFilePaths.isEmpty()) {
			throw new ReleaseFileGenerationException("Failed to find any transformed files to convert to output delta files.");
		}

		for (final String fileName : transformedFilePaths) {
			if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)
					&& fileName.contains(RF2Constants.DELTA)) {
				validFiles.add(fileName);
			}
		}
		if (validFiles.isEmpty()) {
			throw new ReleaseFileGenerationException("Failed to find any files of type *Delta*.txt transformed in build:" + build.getUniqueId());
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
		// Replace Delta_ to Snapshot_ add file name separator due to some extension refsets have the word "delta" as part of the refset name
		return deltaFilename.replace(DELTA + FILE_NAME_SEPARATOR, fullOrSnapshot + FILE_NAME_SEPARATOR)
				.replace(DELTA + HYPHEN, fullOrSnapshot + HYPHEN);
	}
}
