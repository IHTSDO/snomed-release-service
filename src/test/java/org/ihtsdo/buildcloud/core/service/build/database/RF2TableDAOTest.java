package org.ihtsdo.buildcloud.core.service.build.database;

import org.ihtsdo.buildcloud.core.service.build.database.hsql.RF2TableDAOHsqlImpl;
import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RF2TableDAOTest {

	private RF2TableExportDAO rf2TableDAO;
	private String rf2FullFilename;
	private String rf2DeltaFilename;
	private String rf2CoreFullFilename;

	@Before
	public void setup() throws Exception {
		rf2FullFilename = "der2_Refset_SimpleFull_INT_20130630.txt";
		rf2DeltaFilename = "rel2_Refset_SimpleDelta_INT_20130930.txt";
		rf2CoreFullFilename = "sct2_Description_Full-en_INT_20140131.txt";
	}

	@Test
	public void testCreateTableHsql() throws Exception {
		rf2TableDAO = new RF2TableDAOHsqlImpl("test");
		testCreateTable();
	}

	@Test
	public void testCreateTableTreeMap() throws Exception {
		rf2TableDAO = new RF2TableExportDAOImpl(null);
		testCreateTable();
	}

	@Test
	public void testAppendDataHsql() throws Exception {
		rf2TableDAO = new RF2TableDAOHsqlImpl("test");
		testAppendData();
	}

	@Test
	public void testAppendDataMap() throws Exception {
		rf2TableDAO = new RF2TableExportDAOImpl(null);
		testAppendData();
	}

	@Test
	public void testCoreDescCreateTableHsql() throws Exception {
		rf2TableDAO = new RF2TableDAOHsqlImpl("test");
		testCoreDescCreateTable();
	}

	@Test
	public void testCoreDescCreateTableTreeMap() throws Exception {
		rf2TableDAO = new RF2TableExportDAOImpl(null);
		testCoreDescCreateTable();
	}

	private void testCreateTable() throws Exception {
		TableSchema table = rf2TableDAO.createTable(rf2FullFilename, getClass().getResourceAsStream(rf2FullFilename), false);

		RF2TableResults results = rf2TableDAO.selectAllOrdered(table);

		Assert.assertEquals("a895084b-10bc-42ca-912f-d70e8f0b825e\t20130130\t1\t900000000000207008\t450990004\t293495006", results.nextLine());
		Assert.assertEquals("beae078d-9e5b-4b15-a8b1-9260705afce2\t20130130\t1\t900000000000207008\t450990004\t293507007", results.nextLine());
		Assert.assertEquals("beae078d-9e5b-4b15-a8b1-9260705afce2\t20130630\t0\t900000000000207008\t450990004\t293507007", results.nextLine());
		Assert.assertEquals("347a0a38-98ab-481c-8974-fcaa6e46385c\t20130130\t1\t900000000000207008\t450990004\t62014003", results.nextLine());
		Assert.assertEquals("347a0a38-98ab-481c-8974-fcaa6e46385c\t20130630\t0\t900000000000207008\t450990004\t62014003", results.nextLine());
		Assert.assertEquals("5fa7d98a-2010-4490-bc87-7dce3a540d04\t20131230\t1\t900000000000207008\t450990004\t293104123", results.nextLine());
		Assert.assertNull(results.nextLine());
	}

	private void testAppendData() throws Exception {
		TableSchema tableSchema = rf2TableDAO.createTable(rf2FullFilename, getClass().getResourceAsStream(rf2FullFilename), false);

		rf2TableDAO.appendData(tableSchema, getClass().getResourceAsStream(rf2DeltaFilename), false);

		RF2TableResults results = rf2TableDAO.selectAllOrdered(tableSchema);
		Assert.assertEquals("a895084b-10bc-42ca-912f-d70e8f0b825e\t20130130\t1\t900000000000207008\t450990004\t293495006", results.nextLine());
		Assert.assertEquals("beae078d-9e5b-4b15-a8b1-9260705afce2\t20130130\t1\t900000000000207008\t450990004\t293507007", results.nextLine());
		Assert.assertEquals("beae078d-9e5b-4b15-a8b1-9260705afce2\t20130630\t0\t900000000000207008\t450990004\t293507007", results.nextLine());
		Assert.assertEquals("beae078d-9e5b-4b15-a8b1-9260705afce2\t20130930\t1\t900000000000207008\t450990004\t293507007", results.nextLine());
		Assert.assertEquals("347a0a38-98ab-481c-8974-fcaa6e46385c\t20130130\t1\t900000000000207008\t450990004\t62014003", results.nextLine());
		Assert.assertEquals("347a0a38-98ab-481c-8974-fcaa6e46385c\t20130630\t0\t900000000000207008\t450990004\t62014003", results.nextLine());
		Assert.assertEquals("4a926393-55f8-4cdf-95f6-d70c23185212\t20130930\t1\t900000000000207008\t450990004\t293104009", results.nextLine());
		Assert.assertEquals("5fa7d98a-2010-4490-bc87-7dce3a540d04\t20131230\t1\t900000000000207008\t450990004\t293104123", results.nextLine());
		Assert.assertNull(results.nextLine());
	}

	private void testCoreDescCreateTable() throws Exception {
		TableSchema table = rf2TableDAO.createTable(rf2CoreFullFilename, getClass().getResourceAsStream(rf2CoreFullFilename), false);

		RF2TableResults results = rf2TableDAO.selectAllOrdered(table);

		Assert.assertEquals("298405701\t20140131\t1\t900000000000207008\t699400005\ten\t900000000000013009\tDéjá vu\t900000000000020002", results.nextLine());
		Assert.assertEquals("2972087017\t20140131\t1\t900000000000207008\t166855002\ten\t900000000000003001\tLipoprotein electrophoresis - Low density lipoprotein (procedure)\t900000000000020002", results.nextLine());
		Assert.assertEquals("2981505010\t20140131\t1\t900000000000207008\t698906002\ten\t900000000000003001\tMain spoken language Kashmiri (finding)\t900000000000020002", results.nextLine());
		Assert.assertNull(results.nextLine());
	}

	@After
	public void tearDown() throws Exception {
		rf2TableDAO.closeConnection();
	}

}
