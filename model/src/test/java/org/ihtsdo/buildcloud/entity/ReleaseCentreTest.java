package org.ihtsdo.buildcloud.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseCentreTest {

	@Test
	public void testWebId() {
		Assert.assertEquals("international", new ReleaseCentre("International Release Centre", "International").getBusinessKey());
		Assert.assertEquals("other", new ReleaseCentre("The Other One", "Other").getBusinessKey());
		Assert.assertEquals("kais", new ReleaseCentre("(Kai's)", "(Kai's)").getBusinessKey());
	}

}
