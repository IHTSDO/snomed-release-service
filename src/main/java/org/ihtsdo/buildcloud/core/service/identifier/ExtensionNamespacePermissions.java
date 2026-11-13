package org.ihtsdo.buildcloud.core.service.identifier;

import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads extension namespaces from RF2 Delta component ids and reports which of them
 * the CIS account is not allowed to publish.
 */
public final class ExtensionNamespacePermissions {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionNamespacePermissions.class);

	public static final String INTERNATIONAL_NAMESPACE = "0";

	public static final String MISSING_PERMISSION_MESSAGE =
			"Please configure permissions to publish to the following namespaces in CIS: ";

	private ExtensionNamespacePermissions() {
	}

	/**
	 * Namespace of an SCTID. International component ids (partition identifier starting with 0)
	 * return {@code "0"}. Extension ids return the 7-digit namespace with leading zeros removed.
	 * Returns null when the value is not an SCTID or the partition identifier is not recognised.
	 */
	public static String fromSctId(String sctId) {
		if (sctId == null) {
			return null;
		}
		String id = sctId.trim();
		if (id.length() < 3 || !isAllDigits(id)) {
			return null;
		}
		String partitionId = id.substring(id.length() - 3, id.length() - 1);
		if (partitionId.charAt(0) == '0') {
			return INTERNATIONAL_NAMESPACE;
		}
		if (partitionId.charAt(0) == '1') {
			if (id.length() < 10) {
				LOGGER.error("Invalid extension sctId:{}", id);
				return null;
			}
			return Long.toString(Long.parseLong(id.substring(id.length() - 10, id.length() - 3)));
		}
		LOGGER.error("Invalid partition id:{} for sctId:{}", partitionId, id);
		return null;
	}

	/**
	 * Adds extension namespaces (other than {@code 0}) found in the component id column.
	 * Closes {@code inputStream} when reading finishes.
	 */
	public static void collectFromDelta(InputStream inputStream, Set<String> extensionNamespaces) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8))) {
			String line = reader.readLine();
			if (line != null) {
				line = reader.readLine();
				while (line != null) {
					collectExtensionNamespace(line, extensionNamespaces);
					line = reader.readLine();
				}
			}
		}
	}

	private static void collectExtensionNamespace(String line, Set<String> extensionNamespaces) throws IOException {
		if (line.isEmpty()) {
			return;
		}
		int tab = line.indexOf(RF2Constants.COLUMN_SEPARATOR);
		String componentId = tab < 0 ? line.trim() : line.substring(0, tab).trim();
		if (componentId.isEmpty()) {
			return;
		}
		if (!isAllDigits(componentId)) {
			throw new IOException("Delta row has a non-numeric component id: " + abbreviate(componentId));
		}
		String namespaceId = fromSctId(componentId);
		if (namespaceId != null && !INTERNATIONAL_NAMESPACE.equals(namespaceId)) {
			extensionNamespaces.add(namespaceId);
		}
	}

	/**
	 * @return the Ops message when any required extension namespace is not permitted, otherwise null
	 */
	public static String missingPermissionMessage(Collection<String> requiredNamespaces, Collection<String> permittedNamespaces) {
		Set<String> permitted = new HashSet<>();
		if (permittedNamespaces != null) {
			for (String namespace : permittedNamespaces) {
				String normalized = normalize(namespace);
				if (normalized != null) {
					permitted.add(normalized);
				}
			}
		}
		List<String> missing = requiredNamespaces.stream()
				.map(ExtensionNamespacePermissions::normalize)
				.filter(Objects::nonNull)
				.filter(namespace -> !INTERNATIONAL_NAMESPACE.equals(namespace))
				.filter(namespace -> !permitted.contains(namespace))
				.distinct()
				.sorted(Comparator.comparingLong(Long::parseLong))
				.toList();
		if (missing.isEmpty()) {
			return null;
		}
		return MISSING_PERMISSION_MESSAGE + String.join(", ", missing);
	}

	public static String format(Collection<String> namespaces) {
		return namespaces.stream()
				.map(ExtensionNamespacePermissions::normalize)
				.filter(Objects::nonNull)
				.distinct()
				.sorted(Comparator.comparingLong(Long::parseLong))
				.collect(Collectors.joining(", "));
	}

	private static String normalize(String namespace) {
		if (namespace == null) {
			return null;
		}
		String trimmed = namespace.trim();
		if (trimmed.isEmpty() || !isAllDigits(trimmed)) {
			return null;
		}
		return Long.toString(Long.parseLong(trimmed));
	}

	private static boolean isAllDigits(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isDigit(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static String abbreviate(String value) {
		if (value.length() <= 40) {
			return value;
		}
		return value.substring(0, 40) + "...";
	}
}
