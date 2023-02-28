package org.ihtsdo.buildcloud.core.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseCenterTest {

	@Test
	public void testBusinessKey() {
		assertEquals("international", new ReleaseCenter("International Release Center", "International").getBusinessKey());
		assertEquals("other", new ReleaseCenter("The Other One", "Other").getBusinessKey());
		assertEquals("kais", new ReleaseCenter("(Kai's)", "(Kai's)").getBusinessKey());
	}

}
