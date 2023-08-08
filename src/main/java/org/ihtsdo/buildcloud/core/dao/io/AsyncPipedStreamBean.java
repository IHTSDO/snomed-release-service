package org.ihtsdo.buildcloud.core.dao.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Facilitates the streaming upload of a file which is still being written.
 */
public class AsyncPipedStreamBean implements Closeable {

	private final OutputStream outputStream;
	private final Future<String> future;
	private final String outputFilePath;
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPipedStreamBean.class);

	public AsyncPipedStreamBean(OutputStream outputStream, Future<String> future, String outputFilePath) {
		this.outputStream = outputStream;
		this.future = future;
		this.outputFilePath = outputFilePath;
	}

	public void waitForFinish() throws ExecutionException, InterruptedException {
		future.get();
	}

	@Override
	public void close() throws IOException {
		if (outputStream != null && future != null) {
			try {
				waitForFinish();
			} catch (Exception e) {
				throw new IOException("Error while waiting for async stream to finish.", e);
			} finally {
				outputStream.close();
				LOGGER.debug("Finished writing stream {}", outputFilePath);
			}
		}
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public String getOutputFilePath() {
		return this.outputFilePath;
	}

}
