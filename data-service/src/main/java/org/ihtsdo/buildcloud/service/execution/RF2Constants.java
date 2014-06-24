package org.ihtsdo.buildcloud.service.execution;

import java.nio.charset.Charset;

import org.apache.commons.lang3.time.FastDateFormat;

public class RF2Constants {

	public static final String COLUMN_SEPARATOR = "\t";
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String LINE_ENDING = "\r\n";
	public static final String TXT_FILE_EXTENSION = ".txt";
	public static final String DELTA = "Delta";
	public static final String FULL = "Full";
	public static final String SNAPSHOT = "Snapshot";
	public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd");
	public static final String EFFECTIVE_TIME = "effectiveTime";
	public static final String MANIFEST_CONTEXT_PATH = "org.ihtsdo.buildcloud.manifest";
	public static final String FILE_NAME_SEPARATOR = "_";

}
