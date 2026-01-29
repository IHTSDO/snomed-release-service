package org.ihtsdo.buildcloud.core.service.build.database;

import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.buildcloud.core.service.build.database.map.UUIDKey;
import org.ihtsdo.buildcloud.core.service.build.database.map.Key;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Manual performance scenario for comparing exportDelta before/after isIgnoredKey optimisations.
 * How to run (example):
 * mvn -Dtest=Rf2FileWriterPerformanceManualTest -Dperf=true -Drows=200000 -Ddiscard=5000 test

 * Notes:
 * - Disabled by default so CI won't run it (enable with -Dperf=true).
 * - Uses synthetic RF2 delta content to avoid large binary fixtures in git.
 */
class Rf2FileWriterPerformanceManualTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(Rf2FileWriterPerformanceManualTest.class);

	private static final String FILENAME = "der2_Refset_SimpleDelta_INT_20200131.txt";
	private static final String EFFECTIVE_TIME = "20200131";
	private static final String HEADER = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

	@Test
	@EnabledIfSystemProperty(named = "perf", matches = "true")
    void exportDelta_largeDataSet() throws Exception {
		int rows = Integer.getInteger("rows", 200_000);
		int discard = Integer.getInteger("discard", 150_000);
		if (discard > rows) {
			throw new IllegalArgumentException("discard must be <= rows. rows=" + rows + " discard=" + discard);
		}

		LOGGER.info("Preparing dataset. rows={} discard={}", rows, discard);

		// Build an in-memory RF2 delta file (header + rows).
		StringBuilder rf2 = new StringBuilder((HEADER.length() + 1) * (rows + 1));
		rf2.append(HEADER).append("\n");

		// Pre-generate IDs so we can create keysToDiscard that exactly match some rows.
		String[] ids = new String[rows];
		for (int i = 0; i < rows; i++) {
			// UUID generation is intentional here: the typical refset member id is a UUID and
			// it avoids any bias from numeric-only parsing.
			ids[i] = UUID.randomUUID().toString();
		}

		for (int i = 0; i < rows; i++) {
			// Simple refset line: id, effectiveTime, active, moduleId, refsetId, referencedComponentId
			rf2.append(ids[i]).append('\t')
					.append(EFFECTIVE_TIME).append('\t')
					.append('1').append('\t')
					.append("900000000000207008").append('\t')
					.append("450990004").append('\t')
					.append("293507007").append('\n');
		}

		Set<Key> keysToDiscard = new HashSet<>(discard * 2);
		for (int i = 0; i < discard; i++) {
			keysToDiscard.add(new UUIDKey(ids[i], EFFECTIVE_TIME));
		}

		RF2TableExportDAO rf2TableDAO = new RF2TableExportDAOImpl(null);
		Rf2FileWriter rf2FileWriter = new Rf2FileWriter();

		// Parse into table once (we're benchmarking exportDelta filtering, not parsing).
		TableSchema tableSchema = rf2TableDAO.createTable(FILENAME, new ByteArrayInputStream(rf2.toString().getBytes(StandardCharsets.UTF_8)), false);

		// Warmup
		for (int i = 0; i < 2; i++) {
			ByteArrayOutputStream warmupOut = new ByteArrayOutputStream();
			rf2FileWriter.exportDelta(rf2TableDAO.selectAllOrdered(tableSchema), tableSchema, warmupOut, keysToDiscard);
		}

		// Measured run
		ByteArrayOutputStream deltaOutputStream = new ByteArrayOutputStream();
		long start = System.nanoTime();
		rf2FileWriter.exportDelta(rf2TableDAO.selectAllOrdered(tableSchema), tableSchema, deltaOutputStream, keysToDiscard);
		long tookMs = (System.nanoTime() - start) / 1_000_000;

		LOGGER.info("TookMs={}", tookMs);
		// Basic sanity check: output should be header + (rows - discard) lines
		int expectedLines = 1 + (rows - discard);
		int actualLines = countLines(deltaOutputStream);

		LOGGER.info("exportDelta completed. tookMs={} expectedLines={} actualLines={} bytes={}",
				tookMs, expectedLines, actualLines, deltaOutputStream.size());

		assertEquals(expectedLines, actualLines);

		rf2TableDAO.closeConnection();
	}

	private int countLines(ByteArrayOutputStream out) {
		// Count '\n' occurrences; exportDelta writes LF line endings in this code path.
		byte[] bytes = out.toByteArray();
		int lines = 0;
		for (byte b : bytes) {
			if (b == (byte) '\n') {
				lines++;
			}
		}
		// If file doesn't end with '\n' (should), count the last line.
		if (bytes.length > 0 && bytes[bytes.length - 1] != (byte) '\n') {
			lines++;
		}
		return lines;
	}
}

