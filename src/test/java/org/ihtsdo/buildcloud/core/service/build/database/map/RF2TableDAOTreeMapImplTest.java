package org.ihtsdo.buildcloud.core.service.build.database.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.core.service.build.database.RF2TableExportDAO;
import org.ihtsdo.buildcloud.core.service.build.database.RF2TableResults;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class RF2TableDAOTreeMapImplTest {

	private RF2TableExportDAO dao;
	private Map<String, List<Integer>> customRefsetCompositeKeys;

	@BeforeEach
	public void setUp() throws Exception {
		customRefsetCompositeKeys = new HashMap<>();
		dao = new RF2TableExportDAOImpl(customRefsetCompositeKeys);
	}

	@Test
	public void testReconcileRefsetMemberIds_simpleRefset() throws Exception {
		String effectiveTime = "20140731";

		String deltaInput = "rel2_Refset_SimpleDelta_INT_20140731.txt";
		Class<?> thisClass = getClass();
		TableSchema tableSchema = dao.createTable(deltaInput, thisClass.getResourceAsStream(deltaInput), true);

		String previousSnapshot = "der2_Refset_SimpleSnapshot_INT_20140131.txt";
		dao.reconcileRefsetMemberIds(thisClass.getResourceAsStream(previousSnapshot), previousSnapshot, effectiveTime);

		String expectedNewDelta = "der2_Refset_SimpleDelta_INT_20140731.txt";
		RF2TableResults results = dao.selectAllOrdered(tableSchema);
		StreamTestUtils.assertStreamsEqualLineByLine(expectedNewDelta, thisClass.getResourceAsStream(expectedNewDelta),
				new RF2TableResultsReaderHack(results));
	}

	@Test
	public void testReconcileRefsetMemberIds_AssociationReferenceRefset() throws Exception {
		String effectiveTime = "20140731";

		String deltaInput = "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		Class<?> thisClass = getClass();
		TableSchema tableSchema = dao.createTable(deltaInput, thisClass.getResourceAsStream(deltaInput), true);

		String previousSnapshot = "der2_cRefset_AssociationReferenceSnapshot_INT_20140131.txt";
		dao.reconcileRefsetMemberIds(thisClass.getResourceAsStream(previousSnapshot), previousSnapshot, effectiveTime);

		String expectedNewDelta = "der2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		RF2TableResults results = dao.selectAllOrdered(tableSchema);
		StreamTestUtils.assertStreamsEqualLineByLine(expectedNewDelta, thisClass.getResourceAsStream(expectedNewDelta),
				new RF2TableResultsReaderHack(results));
	}

	@Test
	public void testReconcileRefsetMemberIds_AssociationReferenceRefsetCustomKey() throws Exception {
		customRefsetCompositeKeys.put("450990004", Lists.newArrayList(3, 4));
		String effectiveTime = "20140731";

		String deltaInput = "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		Class<?> thisClass = getClass();
		TableSchema tableSchema = dao.createTable(deltaInput, thisClass.getResourceAsStream(deltaInput), true);

		String previousSnapshot = "der2_cRefset_AssociationReferenceSnapshot_INT_20140131.txt";
		dao.reconcileRefsetMemberIds(thisClass.getResourceAsStream(previousSnapshot), previousSnapshot, effectiveTime);

		String expectedNewDelta = "der2_cRefset_AssociationReferenceDelta_INT_20140731_custom_key.txt";
		RF2TableResults results = dao.selectAllOrdered(tableSchema);
		
		StreamTestUtils.assertStreamsEqualLineByLine(expectedNewDelta, thisClass.getResourceAsStream(expectedNewDelta),
				new RF2TableResultsReaderHack(results));
	}

	@Test
	public void testSelectDeltaFromSnapshot() throws Exception {
		String newSnapshot = "der2_cRefset_AssociationReferenceSnapshot_D_20140731.txt.txt";

		TableSchema table = dao.createTable(newSnapshot, getClass().getResourceAsStream(newSnapshot), false);

		String expectedNewDelta = "der2_cRefset_AssociationReferenceDelta_D_20140731.txt";
		RF2TableResults results = dao.selectWithEffectiveDateOrdered(table, "20140731");
		StreamTestUtils.assertStreamsEqualLineByLine(expectedNewDelta, getClass().getResourceAsStream(expectedNewDelta),
				new RF2TableResultsReaderHack(results));
	}

	static class RF2TableResultsReaderHack extends BufferedReader {

		private final RF2TableResults results;

		public RF2TableResultsReaderHack(RF2TableResults results) {
			super(new StringReader(""));
			this.results = results;
		}

		@Override
		public String readLine() throws IOException {
			try {
				return results.nextLine();
			} catch (SQLException e) {
				throw new IOException(e);
			}
		}

	}

}
