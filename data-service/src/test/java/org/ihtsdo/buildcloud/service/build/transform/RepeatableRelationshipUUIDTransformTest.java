package org.ihtsdo.buildcloud.service.build.transform;

import java.security.NoSuchAlgorithmException;

import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RepeatableRelationshipUUIDTransformTest {

	private RepeatableRelationshipUUIDTransform transform;

	@Before
	public void setup() throws NoSuchAlgorithmException {
		transform = new RepeatableRelationshipUUIDTransform();
	}

	@Test
	public void testTransformSameLineAlwaysGivesSameUUID() throws Exception {
		final String lineA = "null\t20140731\t1\t900000000000207008\t1016002\t81137002\t0\t116680003\t900000000000010007\t900000000000451002";
		final String lineB = "null\t20140731\t1\t900000000000012004\t800010001\t399748002\t0\t116680003\t900000000000010007\t900000000000451002";

		final String[] valuesA1 = lineA.split("\\t");
		Assert.assertEquals(RF2Constants.NULL_STRING, valuesA1[0]);
		transform.transformLine(valuesA1);
		Assert.assertEquals("c5c20346-62d2-5fb1-8df4-82fbdbb4cc49", valuesA1[0]);

		final String[] valuesA2 = lineA.split("\\t");
		Assert.assertEquals(RF2Constants.NULL_STRING, valuesA2[0]);
		transform.transformLine(valuesA2);
		Assert.assertEquals("c5c20346-62d2-5fb1-8df4-82fbdbb4cc49", valuesA2[0]);

		final String[] valuesB1 = lineB.split("\\t");
		Assert.assertEquals(RF2Constants.NULL_STRING, valuesB1[0]);
		transform.transformLine(valuesB1);
		Assert.assertEquals("81342da0-c3df-5ce2-900a-95fd6be13885", valuesB1[0]);

		final String[] valuesB2 = lineB.split("\\t");
		Assert.assertEquals(RF2Constants.NULL_STRING, valuesB2[0]);
		transform.transformLine(valuesB2);
		Assert.assertEquals("81342da0-c3df-5ce2-900a-95fd6be13885", valuesB2[0]);
	}
}