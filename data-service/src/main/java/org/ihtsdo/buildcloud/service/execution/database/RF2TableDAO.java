package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;

public interface RF2TableDAO {

	TableSchema createTable(String rf2FilePath, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws SQLException, IOException, FileRecognitionException, ParseException, DatabasePopulatorException;

	void appendData(TableSchema tableSchema, InputStream rf2InputStream, boolean workbenchDataFixesRequired) throws IOException, SQLException, ParseException, DatabasePopulatorException;

	RF2TableResults selectAllOrdered(TableSchema tableSchema) throws SQLException;

	RF2TableResults selectWithEffectiveDateOrdered(TableSchema table, String effectiveDate) throws SQLException;

	RF2TableResults selectNone(TableSchema tableSchema) throws SQLException;

	void closeConnection() throws SQLException;

	/**
	 * This is a workaround for fact that Workbench does not consider the published state when exporting modified content.
	 * If an author has changed and component and then changed it back both states get exported to the release process when neither should.
	 * This functionality should be deleted when the Workbench authoring tool is replaced.
	 *  @param previousSnapshotFileStream InputStream of previous published full RF2 file.
	 * @param currentSnapshotFileName
	 * @param effectiveTime
	 */
	void discardAlreadyPublishedDeltaStates(InputStream previousSnapshotFileStream, String currentSnapshotFileName, String effectiveTime) throws IOException, DatabasePopulatorException;

	/**
	 * This is a workaround for Workbench. Workbench exports refset members with the wrong UUID.
	 * This functionality should be deleted when the Workbench authoring tool is replaced.
	 * @param previousSnapshotFileStream
	 * @param currentSnapshotFileName
	 * @param effectiveTime
	 */
	void reconcileRefsetMemberIds(InputStream previousSnapshotFileStream, String currentSnapshotFileName, String effectiveTime) throws IOException, DatabasePopulatorException;

	/**
	 * This is a workaround for dealing with daily export delta file from WorkBench.
	 * Workbench authoring tool uses a blank value in the 7th column of the AttibuteValue Refset file
	 * to signify component inactivation with "reason not stated"
	 * Note: Empty value id for if new member id which doesn't exist in previous snapshot will not be replaced or removed as it is an invalid
	 * data issue.
	 *
	 * @param previousFileStream
	 * @param effectiveTime
	 * @throws IOException
	 */
	void resolveEmptyValueId(InputStream previousFileStream, String effectiveTime) throws IOException;

	/**
	 * This is a workaround for Workbench. New member ids can only be generated once reconcileRefsetMemberIds has been used to identify
	 * which members are new.
	 * This functionality should be deleted when the Workbench authoring tool is replaced.
	 * @param effectiveTime
	 * @throws DatabasePopulatorException
	 */
	void generateNewMemberIds(String effectiveTime) throws DatabasePopulatorException;

}

