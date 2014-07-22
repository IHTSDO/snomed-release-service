package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.execution.database.DatabaseManager;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.RF2TableDAO;
import org.ihtsdo.buildcloud.service.execution.database.Rf2FileWriter;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Rf2FileExportService {

	private final Execution execution;
	private final Package pkg;
	private final Product product;
	private final ExecutionDAO executionDao;
	private final int maxRetries;
	private static final Logger LOGGER = LoggerFactory.getLogger(Rf2FileExportService.class);

	public Rf2FileExportService(final Execution execution, final Package pkg, ExecutionDAO dao, int maxRetries) {
		this.execution = execution;
		this.pkg = pkg;
		product = pkg.getBuild().getProduct();
		executionDao = dao;
		this.maxRetries = maxRetries;
	}
	
	public final void generateReleaseFiles() {
		boolean firstTimeRelease = pkg.isFirstTimeRelease();
		List<String> transformedFiles = getTransformedDeltaFiles();

		for (String thisFile : transformedFiles) {
			int failureCount = 0;
			boolean success = false;
			do {
				try {
					generateReleaseFile(thisFile, firstTimeRelease);
					success = true;
				} catch (ReleaseFileGenerationException e) {
					failureCount++;
					if (failureCount > maxRetries) {
						throw new ReleaseFileGenerationException("Maximum failure recount of " + maxRetries + " exceeeded.", e);
					} else {
						LOGGER.warn("Failure while processing {} due to {}. Retrying ({})...", thisFile, e.getMessage(), failureCount);
					}
				}
			} while (!success);
		}
	}

	private void generateReleaseFile(String transformedDeltaDataFile, boolean firstTimeRelease) {
		try (Connection connection = getConnection(pkg.getBusinessKey())) {

			// Create table containing transformed input delta
			InputStream transformedDeltaInputStream = executionDao.getTransformedFileAsInputStream(execution,
					pkg.getBusinessKey(), transformedDeltaDataFile);

			RF2TableDAO rf2TableDAO = getDatabasePopulator(connection);
			TableSchema tableSchema = rf2TableDAO.createTable(transformedDeltaDataFile, transformedDeltaInputStream);

			// Export ordered Delta file
			Rf2FileWriter rf2FileWriter = new Rf2FileWriter();
			AsyncPipedStreamBean deltaFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), transformedDeltaDataFile);

			ResultSet deltaResultSet;
			if (firstTimeRelease) {
				deltaResultSet = rf2TableDAO.selectNone(tableSchema);
			} else {
				deltaResultSet = rf2TableDAO.selectAllOrdered(tableSchema);
			}
			rf2FileWriter.exportDelta(deltaResultSet, tableSchema, deltaFileAsyncPipe.getOutputStream());
			deltaFileAsyncPipe.waitForFinish();

			String currentFullFileName = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.FULL);

			if (!firstTimeRelease) {
				// Find previous published package
				String previousPublishedPackage = pkg.getPreviousPublishedPackage();
				ArchiveEntry previousFullFile = executionDao.getPublishedFileArchiveEntry(product, currentFullFileName, previousPublishedPackage);
				if (previousFullFile == null) {
					throw new RuntimeException("No full file equivalent of:  "
							+ currentFullFileName
							+ " found in prevous published package:" + previousPublishedPackage);
				}

				// Append transformed previous full file
				rf2TableDAO.appendData(tableSchema, previousFullFile.getInputStream());
			}

			// Export Full and Snapshot files
			AsyncPipedStreamBean fullFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), currentFullFileName);
			String snapshotOutputFilePath = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			AsyncPipedStreamBean snapshotAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), snapshotOutputFilePath);

			ResultSet fullResultSet = rf2TableDAO.selectAllOrdered(tableSchema);

			rf2FileWriter.exportFullAndSnapshot(fullResultSet, tableSchema,
					pkg.getBuild().getEffectiveTime(), fullFileAsyncPipe.getOutputStream(),
					snapshotAsyncPipe.getOutputStream());
			fullFileAsyncPipe.waitForFinish();
			snapshotAsyncPipe.waitForFinish();
		} catch (Exception e) {
			String errorMsg = "Failed to generate subsequent full and snapshort release files due to " + e.getMessage();
			throw new ReleaseFileGenerationException(errorMsg, e);
		}
	}

	//in case we need to mock out for testing
	RF2TableDAO getDatabasePopulator(Connection connection) throws DatabasePopulatorException {
		return new RF2TableDAO(connection);
	}
	
	Connection getConnection(String uniqueId) throws ClassNotFoundException, SQLException{
		return new DatabaseManager().createConnection(uniqueId);
	}

	/**
	 * @return the transformed delta file name exception if not found.
	 */
	protected List<String> getTransformedDeltaFiles() {
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
