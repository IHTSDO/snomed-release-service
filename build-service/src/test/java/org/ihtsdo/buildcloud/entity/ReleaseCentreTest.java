package org.ihtsdo.buildcloud.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseCentreTest {

	@Test
	public void testWebId() {
		Assert.assertEquals("international", new ReleaseCentre("International").getBusinessKey());
		Assert.assertEquals("the_other_one", new ReleaseCentre("The Other One").getBusinessKey());
		Assert.assertEquals("kais", new ReleaseCentre("(Kai's)").getBusinessKey());
	}

}
