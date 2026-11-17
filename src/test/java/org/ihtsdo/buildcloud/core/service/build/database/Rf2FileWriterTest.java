package org.ihtsdo.buildcloud.core.service.build.database;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.buildcloud.core.service.build.database.map.UUIDKey;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;

class Rf2FileWriterTest {

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
    void setUp() throws Exception {
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
    void testSimpleRefsetExportFullAndSnapshot() throws Exception {
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
    void testSimpleRefsetExportDelta() throws Exception {
		// Prepare test object for this test
		tableSchema = rf2TableDAO.createTable(CURRENT_SIMPLE_REFSET_DELTA, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		// Run target test method
		rf2FileWriter.exportDelta(tableResults, tableSchema, deltaOutputStream, Collections.emptySet());

		// Assert expectations
		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), new ByteArrayInputStream(deltaOutputStream.toByteArray()));
	}

	@Test
    void testSimpleRefsetExportDeltaWithDiscardedKeys() throws Exception {
		// Prepare test object for this test
		tableSchema = rf2TableDAO.createTable(CURRENT_SIMPLE_REFSET_DELTA, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		// Read the input fixture to pick a real (id, effectiveTime) to discard
		String header;
		String firstDataLine;
		String secondDataLine;
		try (InputStream inputStream = getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			header = reader.readLine();
			firstDataLine = reader.readLine();
			secondDataLine = reader.readLine();
		}

		String[] firstLineParts = firstDataLine.split(RF2Constants.COLUMN_SEPARATOR, 3);
		String idToDiscard = firstLineParts[0];
		String effectiveTimeToDiscard = firstLineParts[1];
		Set<org.ihtsdo.buildcloud.core.service.build.database.map.Key> keysToDiscard =
				Collections.singleton(new UUIDKey(idToDiscard, effectiveTimeToDiscard));

		// Run target test method
		rf2FileWriter.exportDelta(tableResults, tableSchema, deltaOutputStream, keysToDiscard);

		// Assert expectations: header + remaining row (the discarded row should be omitted)
		String expected = header + RF2Constants.LINE_ENDING
				+ secondDataLine + RF2Constants.LINE_ENDING;
		StreamTestUtils.assertStreamsEqualLineByLine(new ByteArrayInputStream(expected.getBytes(RF2Constants.UTF_8)),
				new ByteArrayInputStream(deltaOutputStream.toByteArray()));
	}

	/** Testing simple refset export using previous file and delta file
	 * @throws Exception
	 */
	@Test
    void testSimpleRefsetExportWithAppendData() throws Exception {
	    tableSchema = rf2TableDAO.createTable(PREVIOUS_SIMPLE_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_SIMPLE_REFSET_FULL), false);
	    rf2TableDAO.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_SIMPLE_REFSET_DELTA), false);
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
    void testSimpleRefsetSnapshotExportUsingPreviousReleaseDate() throws Exception {
	    tableSchema = rf2TableDAO.createTable(SIMPLE_REFSET_FULL_PLUS_DELTA, getClass().getResourceAsStream(SIMPLE_REFSET_FULL_PLUS_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse(PREVIOUS_RELEASE_DATE), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_SIMPLE_SNAPSHOT_INT_20130630), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	}

	@Test
    void testExtendedMapRefsetExport() throws Exception {
	    tableSchema = rf2TableDAO.createTable(PREVIOUS_EXTENDED_MAP_REFSET_FULL, getClass().getResourceAsStream(PREVIOUS_EXTENDED_MAP_REFSET_FULL), false);
	    rf2TableDAO.appendData(tableSchema, getClass().getResourceAsStream(CURRENT_EXTENDED_MAP_REFSET_DELTA), false);
		RF2TableResults tableResults = rf2TableDAO.selectAllOrdered(tableSchema);

		rf2FileWriter.exportFullAndSnapshot(tableResults, tableSchema, RF2Constants.DATE_FORMAT.parse("20140731"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_SNAPSHOT_INT), new ByteArrayInputStream(snapshotOutputStream.toByteArray()));
	    StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream(EXPECTED_EXTENDED_MAP_FULL_20140731), new ByteArrayInputStream(fullOutputStream.toByteArray()));
	}

	@Test
	void testExcludeAdditionalRelationshipsOmitsThemFromDeltaFullAndSnapshot() throws Exception {
		final String filename = "sct2_Relationship_Delta_INT_20190131.txt";
		final String header = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
		final String inferredHistorical = "100\t20170731\t1\t900000000000207008\t404684003\t138875005\t0\t116680003\t900000000000011006\t900000000000451002";
		final String inferredCurrent = "100\t20190131\t1\t900000000000207008\t404684003\t138875005\t0\t116680003\t900000000000011006\t900000000000451002";
		final String additionalHistorical = "200\t20170731\t0\t900000000000207008\t404684003\t123037004\t0\t116680003\t900000000000227009\t900000000000451002";
		final String additionalCurrent = "300\t20190131\t1\t900000000000207008\t404684003\t123037004\t0\t116680003\t900000000000227009\t900000000000451002";
		final String laterInferred = "400\t20190131\t1\t900000000000207008\t404684003\t138875005\t0\t116680003\t900000000000011006\t900000000000451002";
		final String content = String.join("\n", header, inferredHistorical, additionalHistorical, inferredCurrent, additionalCurrent, laterInferred) + "\n";

		tableSchema = rf2TableDAO.createTable(filename, new ByteArrayInputStream(content.getBytes(RF2Constants.UTF_8)), false);
		Rf2FileWriter ignoringWriter = new Rf2FileWriter(null, null, true);
		ignoringWriter.exportFullAndSnapshot(rf2TableDAO.selectAllOrdered(tableSchema), tableSchema,
				RF2Constants.DATE_FORMAT.parse("20190131"), fullOutputStream, snapshotOutputStream);

		StreamTestUtils.assertStreamsEqualLineByLine(
				new ByteArrayInputStream((header + "\n" + inferredHistorical + "\n" + inferredCurrent + "\n" + laterInferred + "\n").getBytes(RF2Constants.UTF_8)),
				new ByteArrayInputStream(fullOutputStream.toByteArray()));
		StreamTestUtils.assertStreamsEqualLineByLine(
				new ByteArrayInputStream((header + "\n" + inferredCurrent + "\n" + laterInferred + "\n").getBytes(RF2Constants.UTF_8)),
				new ByteArrayInputStream(snapshotOutputStream.toByteArray()));

		ByteArrayOutputStream includedDelta = new ByteArrayOutputStream();
		rf2FileWriter.exportDelta(rf2TableDAO.selectAllOrdered(tableSchema), tableSchema, includedDelta, Collections.emptySet());
		StreamTestUtils.assertStreamsEqualLineByLine(
				new ByteArrayInputStream((header + "\n" + inferredHistorical + "\n" + inferredCurrent + "\n" + additionalHistorical + "\n" + additionalCurrent + "\n" + laterInferred + "\n").getBytes(RF2Constants.UTF_8)),
				new ByteArrayInputStream(includedDelta.toByteArray()));

		ignoringWriter.exportDelta(rf2TableDAO.selectAllOrdered(tableSchema), tableSchema, deltaOutputStream, Collections.emptySet());
		StreamTestUtils.assertStreamsEqualLineByLine(
				new ByteArrayInputStream((header + "\n" + inferredHistorical + "\n" + inferredCurrent + "\n" + laterInferred + "\n").getBytes(RF2Constants.UTF_8)),
				new ByteArrayInputStream(deltaOutputStream.toByteArray()));
	}

	@AfterEach
    void tearDown() throws Exception {
		rf2TableDAO.closeConnection();
	}

}
