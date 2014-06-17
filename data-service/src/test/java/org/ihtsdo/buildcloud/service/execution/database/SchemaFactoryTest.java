package org.ihtsdo.buildcloud.service.execution.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class SchemaFactoryTest {

	private SchemaFactory schemaFactory;

	private String rf2Filename;
	private String headerLine;

	@Before
	public void setUp() throws Exception {
		schemaFactory = new SchemaFactory();
		rf2Filename = "der2_Refset_SimpleDelta_INT_20140831.txt";
		InputStream rf2Stream = getClass().getResourceAsStream(rf2Filename);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(rf2Stream))) {
			headerLine = reader.readLine();
		}
	}

	@Test
	public void testCreateSchemaBeanSimpleRefset() throws Exception {
		TableSchema schemaBean = schemaFactory.createSchemaBean(rf2Filename, headerLine);

		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getName());
		List<TableSchema.Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());
		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.UUID, fields.get(0).getType());
		Assert.assertEquals("referencedComponentId", fields.get(5).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(5).getType());
	}

}
