package org.ihtsdo.buildcloud.helper;

public class EntityHelper {

	public static String formatAsBusinessKey(String name) {
		String businessKey = null;
		if (name != null) {
			businessKey = name.toLowerCase().replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
		}
		return businessKey;
	}

}
