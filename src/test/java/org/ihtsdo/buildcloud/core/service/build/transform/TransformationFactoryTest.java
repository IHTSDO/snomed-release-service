package org.ihtsdo.buildcloud.core.service.build.transform;

import java.util.List;

import org.ihtsdo.snomed.util.rf2.schema.DataType;
import org.ihtsdo.snomed.util.rf2.schema.Field;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransformationFactoryTest {

	private TransformationFactory transformationFactory;
	private TableSchema schemaBean;

	@BeforeEach
	public void setup() throws FileRecognitionException {
		transformationFactory = new TransformationFactory("0","01012014", new CachedSctidFactory(null, null, null, null,1,10), new RandomUUIDGenerator(), "120", "123", 100);
		schemaBean = new SchemaFactory().createSchemaBean("der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt");
		List<Field> fields = schemaBean.getFields();
		assertEquals(13, fields.size());
		assertEquals(DataType.INTEGER, fields.get(6).getType());
	}

	@Test
	public void testGetSteamingFileTransformation() throws Exception {

		StreamingFileTransformation transformation = transformationFactory.getSteamingFileTransformation(schemaBean);

		List<Transformation> lineTransformations = transformation.getTransformations();
		assertEquals(7, lineTransformations.size());
		int index = 0;
		assertTransform(UUIDTransformation.class, 0, lineTransformations.get(index++));
		assertTransform(ReplaceValueLineTransformation.class, 1, lineTransformations.get(index++));
		assertTransform(SCTIDTransformationFromCache.class, 3, lineTransformations.get(index++));
		assertTransform(SCTIDTransformationFromCache.class, 4, lineTransformations.get(index++));
		assertTransform(SCTIDTransformationFromCache.class, 5, lineTransformations.get(index++));
		assertTransform(SCTIDTransformationFromCache.class, 11, lineTransformations.get(index++));
		assertTransform(SCTIDTransformationFromCache.class, 12, lineTransformations.get(index));

	}

	private void assertTransform(Class<? extends Transformation> expectedTransformationClass, int expectedColumnIndex,
			Transformation actualTransformation) {

		assertNotNull(actualTransformation);

		assertEquals(expectedTransformationClass, actualTransformation.getClass(), "Transformation class.");
		assertEquals(expectedColumnIndex, ((LineTransformation)actualTransformation).getColumnIndex(), "Column index.");

	}
}