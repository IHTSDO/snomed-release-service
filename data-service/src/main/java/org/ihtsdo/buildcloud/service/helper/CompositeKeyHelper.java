package org.ihtsdo.buildcloud.service.helper;

import java.util.regex.Pattern;

import org.ihtsdo.buildcloud.dao.helper.ExecutionS3PathHelper;

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

	public static String getPath(String... pathElements) {
		StringBuffer sb = new StringBuffer();
		for (String element : pathElements) {
			sb.append(ExecutionS3PathHelper.SEPARATOR)
					.append(element);
		}
		return sb.toString();
	}

}
