package org.ihtsdo.buildcloud.dao.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncPipedStreamBean implements Closeable {

	private OutputStream outputStream;
	private Future future;

	public AsyncPipedStreamBean(OutputStream outputStream, Future<String> future) {
		this.outputStream = outputStream;
		this.future = future;
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
			}
		}
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

}
