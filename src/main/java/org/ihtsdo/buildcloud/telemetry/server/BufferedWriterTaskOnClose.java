package org.ihtsdo.buildcloud.telemetry.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public final class BufferedWriterTaskOnClose extends BufferedWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(BufferedWriterTaskOnClose.class);

	private final BufferedWriterTask task;

	public BufferedWriterTaskOnClose(final Writer out, final BufferedWriterTask task) {
		super(out);
		this.task = task;
	}

	@Override
	public final void close() throws IOException {
		super.close();
		try {
			LOGGER.debug("Running task after event stream captured.");
			task.run();
		} catch (Exception e) {
			LOGGER.error("Failed to run task after event stream captured.", e);
		}
	}
}
