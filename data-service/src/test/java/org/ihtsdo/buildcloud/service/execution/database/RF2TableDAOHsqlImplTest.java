package org.ihtsdo.buildcloud.service.execution.database;

import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RF2TableDAOHsqlImplTest {

	private RF2TableDAOHsqlImpl rf2TableDAO;
	private String rf2FullFilename;
	private String rf2DeltaFilename;
	private Connection testConnection;

	@Before
	public void setup() throws Exception {
		rf2TableDAO = new RF2TableDAOHsqlImpl("test");
		testConnection = rf2TableDAO.getConnection();
		rf2FullFilename = "der2_Refset_SimpleFull_INT_20130630.txt";
		rf2DeltaFilename = "rel2_Refset_SimpleDelta_INT_20130930.txt";
	}

	@Test
	public void testCreateTable() throws Exception {
		rf2TableDAO.createTable(rf2FullFilename, getClass().getResourceAsStream(rf2FullFilename));

		List<String> tableNames = getTableNames();
		Assert.assertEquals(1, tableNames.size());
		Assert.assertEquals(getExpectedTableName(rf2FullFilename), tableNames.get(0));

		Statement statement = testConnection.createStatement();
		try {
			ResultSet resultSet = statement.executeQuery("select * from " + tableNames.get(0));

			// Test first row values
			Assert.assertTrue(resultSet.first());
			Assert.assertEquals(1, resultSet.getRow());
			int colIndex = 1;
			Assert.assertEquals("a895084b-10bc-42ca-912f-d70e8f0b825e", resultSet.getString(colIndex++));
			Assert.assertEquals("2013-01-30", resultSet.getDate(colIndex++).toString());
			Assert.assertEquals(true, resultSet.getBoolean(colIndex++));
			Assert.assertEquals(900000000000207008L, resultSet.getLong(colIndex++));
			Assert.assertEquals(450990004L, resultSet.getLong(colIndex++));
			Assert.assertEquals(293495006L, resultSet.getLong(colIndex++));

			// Test last row values
			Assert.assertTrue(resultSet.last());
			Assert.assertEquals(6, resultSet.getRow());
			colIndex = 1;

			Assert.assertEquals("5fa7d98a-2010-4490-bc87-7dce3a540d04", resultSet.getString(colIndex++));
			Assert.assertEquals("2013-12-30", resultSet.getDate(colIndex++).toString());
			Assert.assertEquals(true, resultSet.getBoolean(colIndex++));
			Assert.assertEquals(900000000000207008L, resultSet.getLong(colIndex++));
			Assert.assertEquals(450990004L, resultSet.getLong(colIndex++));
			Assert.assertEquals(293104123L, resultSet.getLong(colIndex++));
		} finally {
			statement.close();
		}
	}

	private String getExpectedTableName(String fileName) {
	    if( fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)){
		return fileName.replace(RF2Constants.TXT_FILE_EXTENSION, "").toUpperCase();
	    }
	    return fileName.toUpperCase();
	}

	@Test
	public void testAppendData() throws Exception {
		TableSchema tableSchema = rf2TableDAO.createTable(rf2FullFilename, getClass().getResourceAsStream(rf2FullFilename));

		rf2TableDAO.appendData(tableSchema, getClass().getResourceAsStream(rf2DeltaFilename));

		List<String> tableNames = getTableNames();
		Assert.assertEquals(1, tableNames.size());
		Assert.assertEquals(getExpectedTableName(rf2FullFilename), tableNames.get(0));

		Statement statement = testConnection.createStatement();
		try {
			ResultSet resultSet = statement.executeQuery("select * from " + tableNames.get(0));

			// Test first row values
			Assert.assertTrue(resultSet.first());
			Assert.assertEquals(1, resultSet.getRow());
			int colIndex = 1;
			Assert.assertEquals("a895084b-10bc-42ca-912f-d70e8f0b825e", resultSet.getString(colIndex++));
			Assert.assertEquals("2013-01-30", resultSet.getDate(colIndex++).toString());
			Assert.assertEquals(true, resultSet.getBoolean(colIndex++));
			Assert.assertEquals(900000000000207008L, resultSet.getLong(colIndex++));
			Assert.assertEquals(450990004L, resultSet.getLong(colIndex++));
			Assert.assertEquals(293495006L, resultSet.getLong(colIndex++));

			// Test last row values
			Assert.assertTrue(resultSet.last());
			Assert.assertEquals(8, resultSet.getRow());
			colIndex = 1;
			Assert.assertEquals("4a926393-55f8-4cdf-95f6-d70c23185212", resultSet.getString(colIndex++));
			Assert.assertEquals("2013-09-30", resultSet.getDate(colIndex++).toString());
			Assert.assertEquals(true, resultSet.getBoolean(colIndex++));
			Assert.assertEquals(900000000000207008L, resultSet.getLong(colIndex++));
			Assert.assertEquals(450990004L, resultSet.getLong(colIndex++));
			Assert.assertEquals(293104009L, resultSet.getLong(colIndex++));
		} finally {
			statement.close();
		}
	}

	private List<String> getTableNames() throws SQLException {
		List<String> tableNames = new ArrayList<>();
		ResultSet tables = testConnection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"});
		while (tables.next()) {
			tableNames.add(tables.getString(3));
		}
		return tableNames;
	}

	@After
	public void tearDown() throws Exception {
		rf2TableDAO.closeConnection();
	}

}
