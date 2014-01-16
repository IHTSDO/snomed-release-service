package org.ihtsdo.buildcloud.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseCentreTest {

	@Test
	public void testWebNameConversion() {
		Assert.assertEquals("international", new ReleaseCentre("International").getWebName());
		Assert.assertEquals("the_other_one", new ReleaseCentre("The Other One").getWebName());
		Assert.assertEquals("kais", new ReleaseCentre("(Kai's)").getWebName());
	}

}
