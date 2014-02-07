package org.ihtsdo.buildcloud.service.maven;

import org.ihtsdo.buildcloud.entity.Build;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Date;

public interface MavenExecutor {
	String exec(Build build, ClassPathResource buildFilesDirectory, Date triggerDate) throws IOException;
}
