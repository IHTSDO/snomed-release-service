package org.ihtsdo.buildcloud.service.helper;

import java.util.regex.Pattern;

public class CompositeKeyHelper {

	private static final Pattern LONG_PATTERN = Pattern.compile("\\d+");

	public static Long getId(String compositeKey) {
		String[] keyParts = compositeKey.split("_", 2);
		if (keyParts.length > 0) {
			String idString = keyParts[0];
			if (LONG_PATTERN.matcher(idString).matches()) {
				return Long.parseLong(idString);
			}
		}
		return null;
	}

}
