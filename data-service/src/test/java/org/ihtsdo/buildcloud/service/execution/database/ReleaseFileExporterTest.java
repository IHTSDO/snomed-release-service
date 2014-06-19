package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.StreamTestUtils;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;

public class ReleaseFileExporterTest {

	private String fullPlusDeltaFilename;
	private String expectedFullFilename;
	private String expectedSnapshotFilename;
	private Connection testConnection;
	private TableSchema tableSchema;
	private ReleaseFileExporter releaseFileExporter;
	private ByteArrayOutputStream fullOutputStream;
	private ByteArrayOutputStream snapshotOutputStream;
	private InputStream expectedFullInputStream;
	private InputStream expectedSnapshotInputStream;

	@Before
	public void setUp() throws Exception {
		fullPlusDeltaFilename = "der2_Refset_SimpleFullPlusDelta_INT_20140831.txt";
		expectedFullFilename = "expected-der2_Refset_SimpleFull_INT_20140831.txt";
		expectedSnapshotFilename = "expected-der2_Refset_SimpleSnapshot_INT_20140831.txt";

		testConnection = new DatabaseManager().createConnection("test");
		DatabasePopulator databasePopulator = new DatabasePopulator(testConnection);
		tableSchema = databasePopulator.createTable(fullPlusDeltaFilename, getClass().getResourceAsStream(fullPlusDeltaFilename));
		releaseFileExporter = new ReleaseFileExporter();

		fullOutputStream = new ByteArrayOutputStream();
		snapshotOutputStream = new ByteArrayOutputStream();
		expectedFullInputStream = getClass().getResourceAsStream(expectedFullFilename);
		expectedSnapshotInputStream = getClass().getResourceAsStream(expectedSnapshotFilename);
	}

	@Test
	public void testExportFullIncFuture() throws Exception {
		releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse("20130630"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(expectedFullInputStream, new ByteArrayInputStream(fullOutputStream.toByteArray()));
	}

	@Test
	public void testExportSnapshot() throws Exception {
		releaseFileExporter.exportFullAndSnapshot(testConnection, tableSchema, RF2Constants.DATE_FORMAT.parse("20130930"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(expectedSnapshotInputStream, new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@After
	public void tearDown() throws Exception {
		testConnection.close();
	}

}
