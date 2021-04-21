package org.ihtsdo.buildcloud.entity;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseCenterTest {

	@Test
	public void testBusinessKey() {
		Assert.assertEquals("international", new ReleaseCenter("International Release Center", "International").getBusinessKey());
		Assert.assertEquals("other", new ReleaseCenter("The Other One", "Other").getBusinessKey());
		Assert.assertEquals("kais", new ReleaseCenter("(Kai's)", "(Kai's)").getBusinessKey());
	}

}
