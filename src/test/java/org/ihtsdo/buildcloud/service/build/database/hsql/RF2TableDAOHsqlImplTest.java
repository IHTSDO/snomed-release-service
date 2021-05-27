package org.ihtsdo.buildcloud.service.build.database.hsql;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;

@RunWith(JMockit.class)
public class RF2TableDAOHsqlImplTest {

	@Mocked
	PreparedStatement insertStatement;

	@Test
	@Ignore
	public void testFullColumns() throws IOException, SQLException, ParseException {
		String testData = "one\ttwo\tthree\tfour\n" + "one\ttwo\tthree\tfour\n";
		runTest(testData);
	}

	@Test
	@Ignore
	public void testBlankLastColumn() throws IOException, SQLException, ParseException {
		String testData = "one\ttwo\tthree\tfour\n" + "one\ttwo\tthree\t\n";
		runTest(testData);
	}

	private void runTest(String testData) throws SQLException, IOException, ParseException {

		RF2TableDAOHsqlImpl testDAO = new RF2TableDAOHsqlImpl();

		String extension = ".N/A";
		final TableSchema testSchema = new TableSchema(ComponentType.CONCEPT, extension);
		testSchema.field("first", DataType.STRING).field("second", DataType.STRING).field("third", DataType.STRING)
				.field("fourth", DataType.STRING);

		// Test data will have first row correct and second row with a blank final column.
		// We expect the same amount of fields to be picked up for both lines

		StringReader testDataSR = new StringReader(testData);
		BufferedReader bufferedTestData = new BufferedReader(testDataSR);

		final int columnCount = testSchema.getFields().size();

		// And announce our expectations for our mocked objects
		new Expectations() {
			{
				insertStatement.setObject(anyInt, anyString);
				times = columnCount;

				insertStatement.addBatch();

				insertStatement.setObject(anyInt, anyString);
				times = columnCount;

				insertStatement.addBatch();

				insertStatement.executeBatch();
			}
		};

		testDAO.insertData(bufferedTestData, testSchema, insertStatement);
	}
}
