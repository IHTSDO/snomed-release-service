package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.buildcloud.entity.ExecutionPackageReport;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StreamingFileTransformation {

	private final List<LineTransformation> lineTransformations;

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingFileTransformation.class);

	public StreamingFileTransformation() {
		lineTransformations = new ArrayList<>();
	}

	public void transformFile(InputStream inputStream, OutputStream outputStream, String fileName, ExecutionPackageReport report)
			throws IOException,
			TransformationException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, RF2Constants.UTF_8));
		try {
			LOGGER.info("Start: Transform file {}.", fileName);
			// Iterate input lines
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			boolean firstLine = true;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (firstLine) {
					firstLine = false;
					writer.write(line);
					writer.write(RF2Constants.LINE_ENDING);
				} else {

					// Split column values
					String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);

					// Pass line through all transformers

					try {
						for (LineTransformation lineTransformation : lineTransformations) {
							lineTransformation.transformLine(columnValues);
						}
					} catch (TransformationException e) {
						LOGGER.warn("TransformationException while processing {} at line {} caused by: {}", fileName, lineNumber,
								e.getMessage());
						report.add("File Transformation", fileName, e.getMessage(), lineNumber);
					}

					// Write transformed line to temp file
					stringBuilder.setLength(0);// reuse StringBuilder
					for (int a = 0; a < columnValues.length; a++) {
						if (a > 0) {
							stringBuilder.append(RF2Constants.COLUMN_SEPARATOR);
						}
						stringBuilder.append(columnValues[a]);
					}
					stringBuilder.append(RF2Constants.LINE_ENDING);

					writer.write(stringBuilder.toString());
				}
			}
			LOGGER.info("Finish: Transform file {}.", fileName);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close writer.", e);
			}
			try {
				reader.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close reader.", e);
			}
		}
	}

	public StreamingFileTransformation addLineTransformation(final LineTransformation transformation) {
		lineTransformations.add(transformation);
		return this;
	}

	public List<LineTransformation> getLineTransformations() {
		return lineTransformations;
	}

}
