package org.ihtsdo.buildcloud.builder;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;

public class MavenExecutorTestManually {

	public void manualTest() throws IOException, InterruptedException {
		MavenExecutor mavenExecutor = new MavenExecutor();

		File buildDirectory = new File("builder/src/test/resources/singlepom");
		System.out.println("Test build directory: " + buildDirectory.getAbsolutePath());
		Assert.assertTrue(buildDirectory.isDirectory());

		int exitValue = mavenExecutor.runMavenProcess(buildDirectory);

		Assert.assertEquals("Process should exit cleanly with a good exit code", 0, exitValue);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		new MavenExecutorTestManually().manualTest();
	}

}