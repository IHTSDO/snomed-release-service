package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Build;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public interface MavenExecutor {
	String exec(Build build, File buildFilesDirectory, Date triggerDate) throws IOException;
}
