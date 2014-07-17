package org.ihtsdo.buildcloud.service.execution.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
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
	private static final String CURRENT_SIMPLE_REFSET_DELTA = "rel2_Refset_SimpleDelta_INT_20130930.txt";
	private static final String CURRENT_EXTENDED_MAP_REFSET_DELTA = "der2_iisssccRefset_ExtendedMapDelta_INT_20140731.txt";
	private static final String EXPECTED_EXTENDED_MAP_SNAPSHOT_INT = "expected-der2_iisssccRefset_ExtendedMapSnapshot_INT_20140731.txt";
	private static final String EXPECTED_EXTENDED_MAP_FULL_20140731 = "expected-der2_iisssccRefset_ExtendedMapFull_INT_20140731.txt";
	
	private Connection testConnection;
	private TableSchema tableSchema;
	private ReleaseFileExporter releaseFileExporter;
	private ByteArrayOutputStream fullOutputStream;
	private ByteArrayOutputStream snapshotOutputStream;
	private  DatabasePopulator databasePopulator;
	private TableSchema simpleRefsetSchema;
	private TableSchema extendedMapSchema;

	@Before
	public void setUp() throws Exception {
	    testConnection = new DatabaseManager().createConnection("test");
	    databasePopulator = new DatabasePopulator(testConnection);
	    releaseFileExporter = new ReleaseFileExporter();
	    fullOutputStream = new ByteArrayOutputStream();
	    snapshotOutputStream = new ByteArrayOutputStream();
		SchemaFactory schemaFactory = new SchemaFactory();
		simpleRefsetSchema = schemaFactory.createSchemaBean(SIMPLE_REFSET_FULL_PLUS_DELTA);
		extendedMapSchema = schemaFactory.createSchemaBean(PREVIOUS_EXTENDED_MAP_REFSET_FULL);
	}

	/** Testing simple refset export using full plus delta file
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExport() throws Exception {
		// Prepare test object for this test
	    tableSchema = databasePopulator.createTable(simpleRefsetSchema, SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA));

		// Run target test method
		releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);

		// Assert expectations
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	/** Testing simple refset export using previous file and delta file 
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExportWithAppendData() throws Exception {
	    tableSchema = databasePopulator.createTable(simpleRefsetSchema, PREVIOUS_SIMPLE_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_SIMPLE_REFSET_FULL));
	    databasePopulator.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA));

		releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);

	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	/**Snapshot file only contains the most recent entries up to the current release date.
	 * No history and no future dated entries.
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetSnapshotExportUsingPreviousReleaseDate() throws Exception {
	    tableSchema = databasePopulator.createTable(simpleRefsetSchema, SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA));

	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse(PREVIOUS_RELEASE_DATE), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_SIMPLE_SNAPSHOT_INT_20130630), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@Test
	public void testExtendedMapRefsetExport() throws Exception {
	    tableSchema = databasePopulator.createTable(extendedMapSchema, PREVIOUS_EXTENDED_MAP_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_EXTENDED_MAP_REFSET_FULL));
	    databasePopulator.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_EXTENDED_MAP_REFSET_DELTA));

	    releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse("20140731"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_SNAPSHOT_INT), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_FULL_20140731), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	}
	@After
	public void tearDown() throws Exception {
		testConnection.close();
	}

}
