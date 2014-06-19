package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.StreamTestUtils;
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
	private Connection testConnection;
	private TableSchema tableSchema;
	private ReleaseFileExporter releaseFileExporter;
	private ByteArrayOutputStream outputStream;
	private InputStream expectedInputStream;

	@Before
	public void setUp() throws Exception {
		fullPlusDeltaFilename = "der2_Refset_SimpleFullPlusDelta_INT_20140831.txt";
		expectedFullFilename = "expected-der2_Refset_SimpleFull_INT_20140831.txt";

		testConnection = new DatabaseManager().createConnection("test");
		DatabasePopulator databasePopulator = new DatabasePopulator(testConnection);
		tableSchema = databasePopulator.createTable(fullPlusDeltaFilename, getClass().getResourceAsStream(fullPlusDeltaFilename));
		releaseFileExporter = new ReleaseFileExporter();

		outputStream = new ByteArrayOutputStream();
		expectedInputStream = getClass().getResourceAsStream(expectedFullFilename);
	}

	@Test
	public void testExportFull() throws Exception {
		releaseFileExporter.exportFull(testConnection, tableSchema, outputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(expectedInputStream, new ByteArrayInputStream(outputStream.toByteArray()));
	}

	@After
	public void tearDown() throws Exception {
		testConnection.close();
	}

}
