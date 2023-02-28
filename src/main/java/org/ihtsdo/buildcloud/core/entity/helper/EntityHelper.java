package org.ihtsdo.buildcloud.core.entity.helper;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;

public class EntityHelper {

	public static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT;

	public static String formatAsBusinessKey(String name) {
		String businessKey = null;
		if (name != null) {
			businessKey = name.toLowerCase().replace(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
		}
		return businessKey;
	}

	public static String formatAsIsoDateTime(Date date) {
		return DATE_FORMAT.format(date);
	}

	public static String formatAsIsoDateTimeURLCompatible(Date date) {
		return formatAsIsoDateTime(date);
	}

}
