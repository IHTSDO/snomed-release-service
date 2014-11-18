package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.buildcloud.entity.ExecutionPackageReport;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StreamingFileTransformation {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingFileTransformation.class);

	private final List<Transformation> transformations;

	private final int transformBufferSize;

	public StreamingFileTransformation(int transformBufferSize) {
		this.transformBufferSize = transformBufferSize;
		transformations = new ArrayList<>();
	}

	public void transformFile(InputStream inputStream, OutputStream outputStream, String fileName, ExecutionPackageReport report)
			throws IOException, TransformationException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, RF2Constants.UTF_8));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, RF2Constants.UTF_8));
		try {
			LOGGER.info("Start: Transform file {}.", fileName);
			// Iterate input lines
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			boolean firstLine = true;
			int lineNumber = 0;
			List<String[]> columnValuesList = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (firstLine) {
					firstLine = false;
					writer.write(line);
					writer.write(RF2Constants.LINE_ENDING);
				} else {

					// Split column values
					String[] columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);

					columnValuesList.add(columnValues);

					if (columnValuesList.size() == transformBufferSize) {
						processLinesInBuffer(columnValuesList, writer, fileName, lineNumber, report, stringBuilder);
						columnValuesList.clear();
					}
				}
			}
			if (!columnValuesList.isEmpty()) {
				processLinesInBuffer(columnValuesList, writer, fileName, lineNumber, report, stringBuilder);
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

	private void processLinesInBuffer(List<String[]> columnValuesList, BufferedWriter writer, String fileName, int lineNumberAtEndOfBatch, ExecutionPackageReport report, StringBuilder stringBuilder) throws IOException {
		int listSize = columnValuesList.size();

		for (Transformation transformation : transformations) {
			if (transformation instanceof BatchLineTransformation) {
				BatchLineTransformation batchLineTransform = (BatchLineTransformation) transformation;
				try {
					batchLineTransform.transformLines(columnValuesList);
				} catch (TransformationException e) {
					int batchLineStart = lineNumberAtEndOfBatch - listSize;
					LOGGER.warn("TransformationException while processing {}, lines in buffer {}-{} caused by: {}", fileName, batchLineStart, lineNumberAtEndOfBatch, e.getMessage());
					report.add("File Transformation", fileName, e.getMessage(), lineNumberAtEndOfBatch);
				}
			} else {
				LineTransformation lineTransformation = (LineTransformation) transformation;
				for (int a = 0; a < listSize; a++) {
					String[] columnValues = columnValuesList.get(a);
					try {
						lineTransformation.transformLine(columnValues);
					} catch (TransformationException e) {
						int currentLineNumber = lineNumberAtEndOfBatch - listSize + a;
						LOGGER.warn("TransformationException while processing {} at line {} caused by: {}", fileName, currentLineNumber, e.getMessage());
						report.add("File Transformation", fileName, e.getMessage(), lineNumberAtEndOfBatch);
					}
				}
			}
		}

		for (String[] columnValues : columnValuesList) {
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

	public StreamingFileTransformation addTransformation(final Transformation transformation) {
		transformations.add(transformation);
		return this;
	}

	public StreamingFileTransformation addTransformationToFrontOfList(final Transformation transformation) {
		transformations.add(0, transformation);
		return this;
	}

	public List<Transformation> getTransformations() {
		return transformations;
	}

}
