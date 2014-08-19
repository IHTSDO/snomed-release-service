package org.ihtsdo.buildcloud.service.execution.database.map;

import org.ihtsdo.buildcloud.service.execution.database.RF2TableResults;
import org.ihtsdo.buildcloud.service.execution.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;

public class RF2TableDAOTreeMapImplTest {

	private RF2TableDAOTreeMapImpl dao;

	@Before
	public void setUp() throws Exception {
		dao = new RF2TableDAOTreeMapImpl(new PesudoUUIDGenerator());
	}

	@Test
	public void testReconcileRefsetMemberIds_simpleRefset() throws Exception {
		String effectiveTime = "20140731";

		String deltaInput = "rel2_Refset_SimpleDelta_INT_20140731.txt";
		Class thisClass = getClass();
		TableSchema tableSchema = dao.createTable(deltaInput, thisClass.getResourceAsStream(deltaInput), true);

		String previousSnapshot = "der2_Refset_SimpleSnapshot_INT_20140131.txt";
		dao.reconcileRefsetMemberIds(thisClass.getResourceAsStream(previousSnapshot), previousSnapshot, effectiveTime);

		dao.generateNewMemberIds(effectiveTime);

		String expectedNewDelta = "der2_Refset_SimpleDelta_INT_20140731.txt";
		RF2TableResults results = dao.selectAllOrdered(tableSchema);
		StreamTestUtils.assertStreamsEqualLineByLine(expectedNewDelta, thisClass.getResourceAsStream(expectedNewDelta),
				new RF2TableResultsReaderHack(results));
	}

	class RF2TableResultsReaderHack extends BufferedReader {

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
