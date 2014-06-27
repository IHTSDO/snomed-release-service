package org.ihtsdo.buildcloud.service.execution.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;

import org.ihtsdo.StreamTestUtils;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReleaseFileExporterTest {

	private static final String PREVIOUS_EXTENDED_MAP_REFSET_FULL = "der2_iisssccRefset_ExtendedMapFull_INT_20140131.txt";
	private static final String PREVIOUS_RELEASE_DATE = "20130630";
	private static final String RELEASE_DATE = "20130930";
	private static final String PREVIOUS_SIMPLE_REFSET_FULL = "der2_Refset_SimpleFull_INT_20130630.txt";
	private static final String EXPECTED_SIMPLE_SNAPSHOT_INT_20130630 = "expected-der2_Refset_SimpleSnapshot_INT_20130630.txt";
	private static final String EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930 = "expected-der2_Refset_SimpleSnapshot_INT_20130930.txt";
	private static final String EXPECTED_REFSET_SIMPLE_FULL_20130930 = "expected-der2_Refset_SimpleFull_INT_20130930.txt";
	private static final String SIMPLE_REFSET_FULL_PLUS_DELTA = "der2_Refset_SimpleFullPlusDelta_INT_20130930.txt";
	private static final String CURRENT_SIMPLE_REFSET_DELTA = "der2_Refset_SimpleDelta_INT_20130930.txt";
	private static final String CURRENT_EXTENDED_MAP_REFSET_DELTA = "der2_iisssccRefset_ExtendedMapDelta_INT_20140731.txt";
	private static final String EXPECTED_EXTENDED_MAP_SNAPSHOT_INT = "expected-der2_iisssccRefset_ExtendedMapSnapshot_INT_20140731.txt";
	private static final String EXPECTED_EXTENDED_MAP_FULL_20140731 = "expected-der2_iisssccRefset_ExtendedMapFull_INT_20140731.txt";
	private Connection testConnection;
	private TableSchema tableSchema;
	private ReleaseFileExporter releaseFileExporter;
	private ByteArrayOutputStream fullOutputStream;
	private ByteArrayOutputStream snapshotOutputStream;
	private InputStream expectedFullInputStream;
	private InputStream expectedSnapshotInputStream;
	private  DatabasePopulator databasePopulator;

	@Before
	public void setUp() throws Exception {
	    testConnection = new DatabaseManager().createConnection("test");
	    databasePopulator = new DatabasePopulator(testConnection);
	    releaseFileExporter = new ReleaseFileExporter();
	    fullOutputStream = new ByteArrayOutputStream();
	    snapshotOutputStream = new ByteArrayOutputStream();
	}
	
	/** Testing simple refset export using full plus delta file
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExport() throws Exception{
	    tableSchema = databasePopulator.createTable(SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA));
	    expectedFullInputStream = getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930);
	    expectedSnapshotInputStream = getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930);
	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedFullInputStream, new ByteArrayInputStream(fullOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedSnapshotInputStream, new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}
	
	
	/** Testing simple refset export using previous file and delta file 
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExportWithAppendData() throws Exception{
	    tableSchema = databasePopulator.createTable(PREVIOUS_SIMPLE_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_SIMPLE_REFSET_FULL));
	    databasePopulator.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA));
	    expectedFullInputStream = getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930);
	    expectedSnapshotInputStream = getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930);
	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedFullInputStream, new ByteArrayInputStream(fullOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedSnapshotInputStream, new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}
	

	/**Snapshot file only contains the most recent entries up to the current release date.
	 * No history and no future dated entries.
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetSnapshotExportUsingPreviousReleaseDate() throws Exception {

	    tableSchema = databasePopulator.createTable(SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA));
	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(PREVIOUS_RELEASE_DATE), fullOutputStream, snapshotOutputStream);
	    expectedSnapshotInputStream = getClass().getResourceAsStream(EXPECTED_SIMPLE_SNAPSHOT_INT_20130630);
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedSnapshotInputStream, new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@Test
	public void testExtendedMapRefsetExport() throws Exception{
	    tableSchema = databasePopulator.createTable(PREVIOUS_EXTENDED_MAP_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_EXTENDED_MAP_REFSET_FULL));
	    databasePopulator.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_EXTENDED_MAP_REFSET_DELTA));
	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse("20140731"), fullOutputStream, snapshotOutputStream);
	    expectedSnapshotInputStream = getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_SNAPSHOT_INT);
	    expectedFullInputStream = getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_FULL_20140731);
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedSnapshotInputStream, new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(expectedFullInputStream, new ByteArrayInputStream(fullOutputStream.toByteArray()));
	}
	
	
	@After
	public void tearDown() throws Exception {
		testConnection.close();
	}

}
