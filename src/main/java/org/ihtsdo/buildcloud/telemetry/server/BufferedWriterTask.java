package org.ihtsdo.buildcloud.telemetry.server;

@FunctionalInterface
public interface BufferedWriterTask {

	void run() throws InterruptedException;
}
