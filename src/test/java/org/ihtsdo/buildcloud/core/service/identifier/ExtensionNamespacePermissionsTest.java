package org.ihtsdo.buildcloud.core.service.identifier;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionNamespacePermissionsTest {

	@Test
	void internationalComponentIdsUseNamespaceZero() {
		assertEquals("0", ExtensionNamespacePermissions.fromSctId("116680003"));
		assertEquals("0", ExtensionNamespacePermissions.fromSctId("900000000000207008"));
		assertEquals("0", ExtensionNamespacePermissions.fromSctId("1016001"));
	}

	@Test
	void extensionComponentIdsExposeTheNamespace() {
		assertEquals("1000288", ExtensionNamespacePermissions.fromSctId("11000288100"));
		assertEquals("1000289", ExtensionNamespacePermissions.fromSctId("21000289105"));
		assertNull(ExtensionNamespacePermissions.fromSctId("12345678925"));
		assertNull(ExtensionNamespacePermissions.fromSctId("not-an-id"));
	}

	@Test
	void deltaScanKeepsExtensionNamespacesOnly() throws Exception {
		String delta = "id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId\n"
				+ "116680003\t20250101\t1\t900000000000207008\t900000000000074008\n"
				+ "11000288100\t20250101\t1\t900000000000207008\t900000000000074008\n"
				+ "21000289105\t20250101\t1\t900000000000207008\t900000000000074008\n"
				+ "11000288101\t20250101\t1\t900000000000207008\t900000000000074008\n";
		Set<String> namespaces = new LinkedHashSet<>();
		ExtensionNamespacePermissions.collectFromDelta(stream(delta), namespaces);

		assertEquals(Set.of("1000288", "1000289"), namespaces);
	}

	@Test
	void internationalDeltaFileHasNoExtensionNamespaces() throws Exception {
		Set<String> namespaces = new LinkedHashSet<>();
		try (InputStream in = getClass().getResourceAsStream(
				"/org/ihtsdo/buildcloud/integration/workbenchworkarround/moduleids/expectedoutput/sct2_Concept_Delta_INT_20140731.txt")) {
			assertNotNull(in);
			ExtensionNamespacePermissions.collectFromDelta(in, namespaces);
		}
		assertTrue(namespaces.isEmpty());
	}

	@Test
	void missingPermissionMessageListsNamespacesInNumericOrder() {
		String message = ExtensionNamespacePermissions.missingPermissionMessage(
				List.of("1000289", "1000288", "0"),
				List.of("0"));

		assertEquals(
				"Please configure permissions to publish to the following namespaces in CIS: 1000288, 1000289",
				message);
	}

	@Test
	void permittedNamespacesSuppressTheError() {
		assertNull(ExtensionNamespacePermissions.missingPermissionMessage(
				List.of("1000288", "01000289"),
				List.of("1000288", "1000289")));
	}

	private static InputStream stream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
}
