package org.ihtsdo.buildcloud.service.execution;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.execution.database.Rf2FileWriter;
import org.ihtsdo.buildcloud.service.execution.database.map.RF2TableDAOTreeMapImpl;
import org.ihtsdo.buildcloud.service.execution.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.helper.StatTimer;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Rf2FileExportService {

	private final Execution execution;
	private final Package pkg;
	private final Product product;
	private final ExecutionDAO executionDao;
	private final int maxRetries;
	private final UUIDGenerator uuidGenerator;
	private static final Logger LOGGER = LoggerFactory.getLogger(Rf2FileExportService.class);
	private final boolean firstTimeRelease;
	private final String effectiveTime;
	private final boolean workbenchDataFixesRequired;
	private final String previousPublishedPackage;

	public Rf2FileExportService(final Execution execution, final Package pkg, ExecutionDAO dao, UUIDGenerator uuidGenerator, int maxRetries) {
		this.execution = execution;
		this.pkg = pkg;
		product = pkg.getBuild().getProduct();
		executionDao = dao;
		this.maxRetries = maxRetries;
		this.uuidGenerator = uuidGenerator;
		firstTimeRelease = pkg.isFirstTimeRelease();
		effectiveTime = pkg.getBuild().getEffectiveTimeSnomedFormat();
		workbenchDataFixesRequired = pkg.isWorkbenchDataFixesRequired();

		if (!firstTimeRelease) {
			// Find previous published package
			previousPublishedPackage = pkg.getPreviousPublishedPackage();
		} else {
			previousPublishedPackage = null;
		}

	}

	public final void generateReleaseFiles() throws ReleaseFileGenerationException {
		List<String> transformedFiles = getTransformedDeltaFiles();
		Set<String> newRF2InputFiles = pkg.getNewRF2InputFileSet();
		for (String thisFile : transformedFiles) {
			int failureCount = 0;
			boolean success = false;
			while (!success) {
				try {
					boolean newFile = newRF2InputFiles.contains(thisFile.replace(RF2Constants.SCT2, RF2Constants.INPUT_FILE_PREFIX).replace(RF2Constants.DER2, RF2Constants.INPUT_FILE_PREFIX));
					boolean fileFirstTimeRelease = newFile || firstTimeRelease;
					generateReleaseFile(thisFile, pkg.getCustomRefsetCompositeKeys(), fileFirstTimeRelease);
					success = true;
				} catch (ReleaseFileGenerationException e) {
					failureCount = handleException(e, thisFile, failureCount);
				}
			}
		}
	}

	public void generateDeltaAndFullFromSnapshot(String snapshotOutputFilename) throws ReleaseFileGenerationException {
		boolean success = false;
		int failureCount = 0;
		while (!success) {
			try {
				generateDeltaAndFull(snapshotOutputFilename);
				success = true;
			} catch (ReleaseFileGenerationException e) {
				failureCount = handleException(e, snapshotOutputFilename, failureCount);
			}
		}
	}

	private void generateReleaseFile(String transformedDeltaDataFile, Map<String, List<Integer>> customRefsetCompositeKeys, final boolean fileFirstTimeRelease) throws ReleaseFileGenerationException {
	    LOGGER.info("Generating release file using {}, isFirstRelease={}", transformedDeltaDataFile, fileFirstTimeRelease);
		StatTimer timer = new StatTimer(getClass());
		RF2TableDAO rf2TableDAO = null;
		TableSchema tableSchema = null;

		try {
			// Create table containing transformed input delta
			LOGGER.debug("Creating table for {}", transformedDeltaDataFile);
			InputStream transformedDeltaInputStream = executionDao.getTransformedFileAsInputStream(execution,
					pkg.getBusinessKey(), transformedDeltaDataFile);

			rf2TableDAO = new RF2TableDAOTreeMapImpl(uuidGenerator, customRefsetCompositeKeys);
			timer.split();
			tableSchema = rf2TableDAO.createTable(transformedDeltaDataFile, transformedDeltaInputStream, workbenchDataFixesRequired);

			String currentSnapshotFileName = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);

			if (workbenchDataFixesRequired) {
				if (!fileFirstTimeRelease) {
					if (tableSchema.getComponentType() == ComponentType.REFSET) {
						// Workbench workaround - correct refset member ids using previous snapshot file.
						// See interface javadoc for more info.
						rf2TableDAO.reconcileRefsetMemberIds(getPreviousFileStream(previousPublishedPackage, currentSnapshotFileName), currentSnapshotFileName, effectiveTime);
					}

					//Workbench workaround for dealing Attribute Value File with empty valueId
					//ideally we should combine all workbench workaround together so that don't read snapshot file twice
					if (transformedDeltaDataFile.contains(RF2Constants.ATTRIBUTE_VALUE_FILE_IDENTIFIER)) {
						rf2TableDAO.resolveEmptyValueId(getPreviousFileStream(previousPublishedPackage,currentSnapshotFileName), effectiveTime);
					}

					// Workbench workaround - use full file to discard invalid delta entries
					// See interface javadoc for more info.
					rf2TableDAO.discardAlreadyPublishedDeltaStates(getPreviousFileStream(previousPublishedPackage, currentSnapshotFileName), currentSnapshotFileName, effectiveTime);
				}

				rf2TableDAO.generateNewMemberIds(effectiveTime);
			}

			LOGGER.debug("Start: Exporting delta file for {}", tableSchema.getTableName());
			timer.setTargetEntity(tableSchema.getTableName());
			timer.logTimeTaken("Create table");

			// Export ordered Delta file
			Rf2FileWriter rf2FileWriter = new Rf2FileWriter();
			AsyncPipedStreamBean deltaFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), transformedDeltaDataFile);

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

			String currentFullFileName = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.FULL);
			if (!fileFirstTimeRelease) {
				InputStream previousFullFileStream = getPreviousFileStream(previousPublishedPackage, currentFullFileName);

				// Append transformed previous full file
				LOGGER.debug("Start: Insert previous release data into table {}", tableSchema.getTableName());
				timer.split();
				rf2TableDAO.appendData(tableSchema, previousFullFileStream, workbenchDataFixesRequired);
				timer.logTimeTaken("Insert previous release data");
				LOGGER.debug("Finish: Insert previous release data into table {}", tableSchema.getTableName());
			}

			// Export Full and Snapshot files
			AsyncPipedStreamBean fullFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), currentFullFileName);
			String snapshotOutputFilePath = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			AsyncPipedStreamBean snapshotAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), snapshotOutputFilePath);

			timer.split();
			RF2TableResults fullResultSet = rf2TableDAO.selectAllOrdered(tableSchema);
			timer.logTimeTaken("selectAllOrdered");

			rf2FileWriter.exportFullAndSnapshot(fullResultSet, tableSchema,
					pkg.getBuild().getEffectiveTime(), fullFileAsyncPipe.getOutputStream(),
					snapshotAsyncPipe.getOutputStream());
			LOGGER.debug("Completed processing full and snapshot files for {}, waiting for network.", tableSchema.getTableName());
			fullFileAsyncPipe.waitForFinish();
			snapshotAsyncPipe.waitForFinish();
		} catch (Exception e) {
			String errorMsg = "Failed to generate subsequent full and snapshort release files due to: " + e.getMessage();
			throw new ReleaseFileGenerationException(errorMsg, e);
		} finally {
			// Clean up time
			if (rf2TableDAO != null) {
				try {
					rf2TableDAO.closeConnection();
				} catch (Exception e) {
					LOGGER.error("Failure while trying to clean up after {}",
							tableSchema != null ? tableSchema.getTableName() : "No table yet.", e);
				}
			}
		}
	}

	private void generateDeltaAndFull(String snapshotOutputFilename) throws ReleaseFileGenerationException {
		String deltaFilename = snapshotOutputFilename.replace(RF2Constants.SNAPSHOT, RF2Constants.DELTA);
		String fullFilename = snapshotOutputFilename.replace(RF2Constants.SNAPSHOT, RF2Constants.FULL);

		// Import snapshot
		InputStream snapshotInputStream = executionDao.getOutputFileInputStream(execution, pkg, snapshotOutputFilename);
		RF2TableDAOTreeMapImpl rf2TableDAO = new RF2TableDAOTreeMapImpl(null, null);
		TableSchema tableSchema;
		try {
			tableSchema = rf2TableDAO.createTable(snapshotOutputFilename, snapshotInputStream, false);
		} catch (IOException | FileRecognitionException | DatabasePopulatorException | NumberFormatException | BadConfigurationException e) {
			throw new ReleaseFileGenerationException("Failed to create table from " + snapshotOutputFilename, e);
		}

		// Export delta
		Rf2FileWriter rf2FileWriter = new Rf2FileWriter();
		RF2TableResults results = rf2TableDAO.selectWithEffectiveDateOrdered(tableSchema, effectiveTime);
		try (AsyncPipedStreamBean deltaOutputStream = executionDao.getOutputFileOutputStream(execution, pkg.getBusinessKey(), deltaFilename)) {
			rf2FileWriter.exportDelta(results, tableSchema, deltaOutputStream.getOutputStream());
		} catch (IOException | SQLException e) {
			throw new ReleaseFileGenerationException("Failed to export " + deltaFilename, e);
		}

		// Import any previous full
		if (!firstTimeRelease) {
			try {
				InputStream previousFullStream = getPreviousFileStream(previousPublishedPackage, fullFilename);
				rf2TableDAO.appendData(tableSchema, previousFullStream, false);
			} catch(IOException | DatabasePopulatorException | BadConfigurationException e) {
				throw new ReleaseFileGenerationException("Failed to import previous full " + fullFilename, e);
			}
		}

		// Export full
		results = rf2TableDAO.selectAllOrdered(tableSchema);
		try (AsyncPipedStreamBean snapshotOutputStream = executionDao.getOutputFileOutputStream(execution, pkg.getBusinessKey(), fullFilename)) {
			rf2FileWriter.exportFull(results, tableSchema, snapshotOutputStream.getOutputStream());
		} catch (IOException | SQLException e) {
			throw new ReleaseFileGenerationException("Failed to export previous full " + fullFilename, e);
		}
	}

	private InputStream getPreviousFileStream(String previousPublishedPackage, String currentFileName) throws IOException {
		InputStream previousFileStream = executionDao.getPublishedFileArchiveEntry(product, currentFileName, previousPublishedPackage);
		if (previousFileStream == null) {
			throw new RuntimeException("No equivalent of:  "
					+ currentFileName
					+ " found in previous published package:" + previousPublishedPackage);
		}
		return previousFileStream;
	}

	private int handleException(ReleaseFileGenerationException e, String thisFile, int failureCount) throws ReleaseFileGenerationException {
		failureCount++;
		// Is this an error that it's worth retrying eg root cause IOException or AWS Related?
		Throwable cause = e.getCause();
		if (failureCount > maxRetries) {
			throw new ReleaseFileGenerationException("Maximum failure recount of " + maxRetries + " exceeeded. Last error: "
					+ e.getMessage(), e);
		} else if (!isNetworkRelated(cause)) {
			// If this isn't something we think we might recover from by retrying, then just re-throw the existing error without
			// modification
			throw e; // TODO: Why are we rethrowing? Should use a subclass of ReleaseFileGenerationException to mark recoverable and only catch those.
		} else {
			LOGGER.warn("Failure while processing {} due to: {}. Retrying ({})...", thisFile, e.getMessage(), failureCount);
		}
		return failureCount;
	}

	private boolean isNetworkRelated(Throwable cause) {
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
	protected List<String> getTransformedDeltaFiles() throws ReleaseFileGenerationException {
		String businessKey = pkg.getBusinessKey();
		List<String> transformedFilePaths = executionDao.listTransformedFilePaths(execution, businessKey);
		List<String> validFiles = new ArrayList<>();
		if (transformedFilePaths.size() < 1) {
			throw new RuntimeException(
					"Failed to find any transformed files to convert to output delta files.");
		}

		for (String fileName : transformedFilePaths) {
			if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)
					&& fileName.contains(RF2Constants.DELTA)) {
				validFiles.add(fileName);
			}
		}
		if (validFiles.size() == 0) {
			throw new ReleaseFileGenerationException(
					"Failed to find any files of type *Delta*.txt transformed in package:"
							+ businessKey);
		}
		return validFiles;
	}
}
