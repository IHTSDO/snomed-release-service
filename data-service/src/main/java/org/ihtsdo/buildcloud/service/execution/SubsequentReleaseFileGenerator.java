package org.ihtsdo.buildcloud.service.execution;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.execution.database.DatabaseManager;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulator;
import org.ihtsdo.buildcloud.service.execution.database.DatabasePopulatorException;
import org.ihtsdo.buildcloud.service.execution.database.ReleaseFileExporter;
import org.ihtsdo.buildcloud.service.execution.database.TableSchema;
import org.ihtsdo.buildcloud.service.file.ArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Release file generator for subsequent release.
 */
public class SubsequentReleaseFileGenerator extends ReleaseFileGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SubsequentReleaseFileGenerator.class);
        
	/**
	 * @param execution an execution.
	 * @param pkg a package name
	 * @param dao  execution dao.
	 */
	public SubsequentReleaseFileGenerator(final Execution execution, final Package pkg, ExecutionDAO dao) {
		super(execution, pkg, dao);
	}

	@Override
	public final void generateReleaseFiles() {
		// get the current transformed delta file
		String transformedDeltDataFile = getTransformedDeltaFile();
		generateDeltaFiles(transformedDeltDataFile, false);
		// load previous full release, add delta input file, export new full
		// file, export new snapshot file
		try (Connection connection = getConnection(pkg.getBusinessKey())) {
			DatabasePopulator databasePopulator = getDatabasePopulator(connection);
			String previousPublishedPackage = pkg.getPreviousPublishedPackage();
			String currentFullFileName = transformedDeltDataFile.replace(RF2Constants.DELTA, RF2Constants.FULL);
			ArchiveEntry previousFullFile = executionDao.getPublishedFileArchiveEntry(product, currentFullFileName, previousPublishedPackage);
			if ( previousFullFile == null ){
			    throw new RuntimeException("No full file found in prevous published package:" + previousPublishedPackage);
			}
			TableSchema tableSchema = databasePopulator.createTable(previousFullFile.getFileName(), previousFullFile.getInputStream());
			InputStream transformedDeltaInputStream = executionDao.getTransformedFileAsInputStream(execution,
							pkg.getBusinessKey(), transformedDeltDataFile);
			databasePopulator.appendData(tableSchema, transformedDeltaInputStream);
			ReleaseFileExporter releaseFileExporter = new ReleaseFileExporter();
			Date targetEffectiveTime = pkg.getBuild().getEffectiveTime();
			AsyncPipedStreamBean fullFileAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), currentFullFileName);

			String currentSnapshot = transformedDeltDataFile.replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT);
			AsyncPipedStreamBean snapshotAsyncPipe = executionDao
					.getOutputFileOutputStream(execution, pkg.getBusinessKey(), currentSnapshot);
			releaseFileExporter.exportFullAndSnapshot(connection, tableSchema,
					targetEffectiveTime, fullFileAsyncPipe.getOutputStream(),
					snapshotAsyncPipe.getOutputStream());
			fullFileAsyncPipe.waitForFinish();
			snapshotAsyncPipe.waitForFinish();
		} catch (Exception e) {
		    	
			String errorMsg = "Failed to generate subsequent full and snapshort release files for package:" + pkg.getBusinessKey();
			LOGGER.error(errorMsg, e);
			throw new ReleaseFileGenerationException(errorMsg, e);
		}
	}

	//in case we need to mock out for testing
	 DatabasePopulator getDatabasePopulator(Connection connection)
		throws DatabasePopulatorException {
	    DatabasePopulator databasePopulator = new DatabasePopulator(connection);
	    return databasePopulator;
	}
	
	 Connection getConnection( String uniqueId) throws ClassNotFoundException, SQLException{
	    return new DatabaseManager().createConnection(uniqueId);
	}
}
