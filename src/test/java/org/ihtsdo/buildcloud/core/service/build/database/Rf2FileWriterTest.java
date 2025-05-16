package org.ihtsdo.buildcloud.core.service.build.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Rf2FileWriterTest {

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

	private TableSchema tableSchema;
	private Rf2FileWriter rf2FileWriter;
	private ByteArrayOutputStream fullOutputStream;
	private ByteArrayOutputStream snapshotOutputStream;
	private ByteArrayOutputStream deltaOutputStream;
	private RF2TableExportDAO rf2TableDAO;

	@BeforeEach
	public void setUp() throws Exception {
	    rf2TableDAO = new RF2TableExportDAOImpl( null);
	    rf2FileWriter = new Rf2FileWriter();
	    fullOutputStream = new ByteArrayOutputStream();
	    snapshotOutputStream = new ByteArrayOutputStream();
		deltaOutputStream = new ByteArrayOutputStream();
	}

	/** Testing simple refset export using full plus delta file
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExportFullAndSnapshot() throws Exception {
		// Prepare test object for this test
		tableSchema = rf2TableDAO.createTable(SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		// Run target test method
		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);

		// Assert expectations
		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930), new ByteArrayInputStream(fullOutputStream.toByteArray()));
		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@Test
	public void testSimpleRefsetExportDelta() throws Exception {
		// Prepare test object for this test
		tableSchema = rf2TableDAO.createTable(CURRENT_SIMPLE_REFSET_DELTA, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		// Run target test method
		rf2FileWriter.exportDelta(tableResults, tableSchema, deltaOutputStream, Collections.emptySet());

		// Assert expectations
		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), new ByteArrayInputStream(deltaOutputStream.toByteArray()));
	}

	/** Testing simple refset export using previous file and delta file 
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetExportWithAppendData() throws Exception {
	    tableSchema = rf2TableDAO.createTable(PREVIOUS_SIMPLE_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_SIMPLE_REFSET_FULL), false);
	    rf2TableDAO.appendData(tableSchema, null, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse(RELEASE_DATE), fullOutputStream, snapshotOutputStream);

	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_FULL_20130930), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_REFSET_SIMPLE_SNAPSHOT_20130930), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	/**Snapshot file only contains the most recent entries up to the current release date.
	 * No history and no future dated entries.
	 * @throws Exception
	 */
	@Test
	public void testSimpleRefsetSnapshotExportUsingPreviousReleaseDate() throws Exception {
	    tableSchema = rf2TableDAO.createTable(SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse(PREVIOUS_RELEASE_DATE), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_SIMPLE_SNAPSHOT_INT_20130630), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@Test
	public void testExtendedMapRefsetExport() throws Exception {
	    tableSchema = rf2TableDAO.createTable(PREVIOUS_EXTENDED_MAP_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_EXTENDED_MAP_REFSET_FULL), false);
	    rf2TableDAO.appendData(tableSchema, null, getClass().getResourceAsStream(CURRENT_EXTENDED_MAP_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse("20140731"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_SNAPSHOT_INT), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_FULL_20140731), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	}

	@AfterEach
	public void tearDown() throws Exception {
		rf2TableDAO.closeConnection();
	}

}
