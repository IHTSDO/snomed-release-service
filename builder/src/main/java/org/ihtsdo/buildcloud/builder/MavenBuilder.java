package org.ihtsdo.buildcloud.builder;

import java.io.File;
import java.io.IOException;

public interface MavenBuilder {

	void exec(File buildFilesDirectory) throws IOException;

}
