package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.execution.database.DatabaseManager;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulator;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.ReleaseFileExporter;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Release file generator for subsequent release.
 */
public class SubsequentReleaseFileGenerator extends ReleaseFileGenerator {
	
	private final Map<String, TableSchema> inputFileSchemaMap;
	private static final Logger LOGGER = LoggerFactory.getLogger(SubsequentReleaseFileGenerator.class);

	/**
	 * @param execution an execution.
	 * @param pkg a package name
	 * @param inputFileSchemaMap
	 * @param dao  execution dao.
	 */
	public SubsequentReleaseFileGenerator(final Execution execution, final Package pkg, Map<String, TableSchema> inputFileSchemaMap, ExecutionDAO dao) {
		super(execution, pkg, dao);
		this.inputFileSchemaMap = inputFileSchemaMap;
	}
	
	@Override
	public final void generateReleaseFiles() {
		List<String> transformedFiles = getTransformedDeltaFiles();
		for (String thisFile : transformedFiles) {
			generateReleaseFile(thisFile);
		}
	}

	private final void generateReleaseFile(String transformedDeltaDataFile) {
		// get the current transformed delta file
		generateDeltaFile(transformedDeltaDataFile, false);

		// load previous full release, add delta input file,
		// export new full and snapshot files
		try (Connection connection = getConnection(pkg.getBusinessKey())) {

			// Find previous published package
			String previousPublishedPackage = pkg.getPreviousPublishedPackage();
			String currentFullFileName = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.FULL);
			ArchiveEntry previousFullFile = executionDao.getPublishedFileArchiveEntry(product, currentFullFileName, previousPublishedPackage);
			if (previousFullFile == null){
				throw new RuntimeException("No full file equivalent of:  "
											+ currentFullFileName
											+ " found in prevous published package:" + previousPublishedPackage);
			}

			// Create table containing previous full file
			String rf2FilePath = previousFullFile.getFilePath();
			TableSchema tableSchema = inputFileSchemaMap.get(transformedDeltaDataFile);
			DatabasePopulator databasePopulator = getDatabasePopulator(connection);
			tableSchema = databasePopulator.createTable(tableSchema, rf2FilePath, previousFullFile.getInputStream());

			// Append transformed input delta
			InputStream transformedDeltaInputStream = executionDao.getTransformedFileAsInputStream(execution,
							pkg.getBusinessKey(), transformedDeltaDataFile);
			databasePopulator.appendData(tableSchema, transformedDeltaInputStream);

			// Export Full and Snapshot files
			ReleaseFileExporter releaseFileExporter = new ReleaseFileExporter();
			AsyncPipedStreamBean fullFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), currentFullFileName);
			String snapshotOutputFilePath = transformedDeltaDataFile.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			AsyncPipedStreamBean snapshotAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), snapshotOutputFilePath);
			releaseFileExporter.exportFullAndSnapshot(connection, tableSchema,
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
	DatabasePopulator getDatabasePopulator(Connection connection) throws DatabasePopulatorException {
		return new DatabasePopulator(connection);
	}
	
	Connection getConnection(String uniqueId) throws ClassNotFoundException, SQLException{
		return new DatabaseManager().createConnection(uniqueId);
	}

}
