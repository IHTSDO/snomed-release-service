package org.ihtsdo.buildcloud.service.execution.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SchemaFactoryTest {

	private SchemaFactory schemaFactory;

	@Before
	public void setUp() throws Exception {
		schemaFactory = new SchemaFactory();
	}

	@Test
	public void testCreateSchemaBeanSimpleRefset() throws Exception {
		String filename = "der2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename, headerLine);

		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getName());
		List<TableSchema.Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.UUID, fields.get(0).getType());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());

		Assert.assertEquals("refSetId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());

		Assert.assertEquals("referencedComponentId", fields.get(5).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(5).getType());
	}

}
