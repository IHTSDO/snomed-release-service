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

/**
 * Release file generator for subsequent release.
 */
public class SubsequentReleaseFileGenerator extends ReleaseFileGenerator {
        
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
			
			//We're looking for the previous full file, so change the filename to be Full
			if (!transformedDeltDataFile.contains(RF2Constants.DELTA)) {
				throw new Exception ("Malformed delta filename encountered: " + transformedDeltDataFile);
			}
			String currentFullFileName = transformedDeltDataFile.replace(RF2Constants.DELTA, RF2Constants.FULL);
			ArchiveEntry previousFullFile = executionDao.getPublishedFile(product, currentFullFileName, previousPublishedPackage);
			
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
			throw new ReleaseFileGenerationException(
					"Failed to generate subsequent full and snapshort release files for package:"
					+ pkg.getBusinessKey(), e);
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
