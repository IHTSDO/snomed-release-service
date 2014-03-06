package org.ihtsdo.buildcloud.service.file;

import java.io.File;
import java.io.IOException;

public class FileUtils {

	public static File createDirectoryOrThrow(File directory) throws IOException {
		if (!directory.mkdirs()) {
			throw new IOException("Unable to create directory " + directory.getAbsolutePath());
		}
		return directory;
	}

}
