package org.ihtsdo.buildcloud.service.execution.database;

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

public class DatabasePopulatorTest {

	private DatabasePopulator databasePopulator;
	private String rf2Filename;
	private Connection testConnection;

	@Before
	public void setup() throws Exception {
		testConnection = new DatabaseManager().createConnection("test");
		databasePopulator = new DatabasePopulator(testConnection);
		rf2Filename = "der2_Refset_SimpleDelta_INT_20140831.txt";
	}

	@Test
	public void testCreateTable() throws Exception {
		databasePopulator.createTable(rf2Filename, getClass().getResourceAsStream(rf2Filename));

		List<String> tableNames = getTableNames();
		Assert.assertEquals(1, tableNames.size());
		Assert.assertEquals("DER2_REFSET_SIMPLEDELTA_INT_20140831", tableNames.get(0));

		Statement statement = testConnection.createStatement();
		try {
			ResultSet resultSet = statement.executeQuery("select * from DER2_REFSET_SIMPLEDELTA_INT_20140831");
			Assert.assertTrue(resultSet.first());
			int col = 1;
			Assert.assertEquals("3570b46b-b581-4655-ba2c-9a677a2e880c", resultSet.getString(col++));
			Assert.assertEquals("2014-01-31", resultSet.getDate(col++).toString());
			Assert.assertEquals(true, resultSet.getBoolean(col++));
			Assert.assertEquals(900000000000207008L, resultSet.getLong(col++));
			Assert.assertEquals(450990004L, resultSet.getLong(col++));
			Assert.assertEquals(293495006L, resultSet.getLong(col++));
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
	public void tearDown() throws SQLException {
		databasePopulator.closeConnection();
	}

}
