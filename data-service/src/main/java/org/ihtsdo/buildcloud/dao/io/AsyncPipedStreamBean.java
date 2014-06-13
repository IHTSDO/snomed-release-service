package org.ihtsdo.buildcloud.dao.io;

import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncPipedStreamBean {

	private OutputStream outputStream;
	private Future future;

	public AsyncPipedStreamBean(OutputStream outputStream, Future<String> future) {
		this.outputStream = outputStream;
		this.future = future;
	}

	public void waitForFinish() throws ExecutionException, InterruptedException {
		future.get();
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

}
