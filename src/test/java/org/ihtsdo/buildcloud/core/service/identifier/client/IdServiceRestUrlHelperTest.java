package org.ihtsdo.buildcloud.core.service.identifier.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdServiceRestUrlHelperTest {

	@Test
	void userNamespacesUrlTargetsTheLoggedInAccount() {
		IdServiceRestUrlHelper helper = new IdServiceRestUrlHelper("https://dev-cis.ihtsdotools.org/api");

		assertEquals(
				"https://dev-cis.ihtsdotools.org/api/users/srs-release/namespaces/?token=abc",
				helper.getUserNamespacesUrl("srs-release", "abc"));
	}
}
