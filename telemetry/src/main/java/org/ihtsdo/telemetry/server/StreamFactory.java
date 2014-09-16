package org.ihtsdo.telemetry.server;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.telemetry.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;

public class StreamFactory {

	private String tempDirectoryPath = "/tmp/telemetry-tmp";
	private final TransferManager transferManager;

	@Autowired
	public StreamFactory(TransferManager transferManager) {
		this.transferManager = transferManager;
		new File(tempDirectoryPath).mkdirs();
	}

	public BufferedWriter createStreamWriter(String correlationID, String streamUri) throws IOException {
		String[] split = streamUri.split("://", 2);
		String protocol = split[0];
		String path = split[1];
		if (Constants.FILE.equals(protocol)) {
			return new BufferedWriter(new FileWriter(path));
		} else if (Constants.s3.equals(protocol)) {

			String[] split1 = path.split("/", 2);
			final String bucketName = split1[0];
			final String objectKey = split1[1];

			String tempFilePath = tempDirectoryPath + "/" + correlationID;

			final File tempFile = new File(tempFilePath);
			return new BufferedWriterTaskOnClose(new FileWriter(tempFile), new Task() {
				@Override
				public void run() throws InterruptedException {
					Upload upload = transferManager.upload(bucketName, objectKey, tempFile);
					upload.waitForUploadResult();
					tempFile.delete();
				}
			});
		} else {
			throw new NotImplementedException("Unrecognised stream URI protocol: " + protocol);
		}
	}

	private static class BufferedWriterTaskOnClose extends BufferedWriter {

		private static final Logger LOGGER = LoggerFactory.getLogger(BufferedWriterTaskOnClose.class);

		private final Task task;

		private BufferedWriterTaskOnClose(Writer out, Task task) {
			super(out);
			this.task = task;
		}

		@Override
		public void close() throws IOException {
			super.close();
			try {
				LOGGER.debug("Running task after event stream captured.");
				task.run();
			} catch (Exception e) {
				LOGGER.error("Failed to run task after event stream captured.", e);
			}
		}
	}

	private static interface Task {

		void run() throws Exception;

	}
}
