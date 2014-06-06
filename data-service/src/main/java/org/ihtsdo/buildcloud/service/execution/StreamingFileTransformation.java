package org.ihtsdo.buildcloud.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class StreamingFileTransformation {

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String LINE_ENDING = "\r\n";

	private List<LineTransformation> lineTransformations;

	private static final String COLUMN_SEPARATOR = "\t";
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingFileTransformation.class);

	public StreamingFileTransformation() {
		lineTransformations = new ArrayList<>();
	}

	public void transformFile(InputStream inputStream, OutputStream outputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8));
		try {
			// Iterate input lines
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					writer.write(line);
					writer.write(LINE_ENDING);
				} else {

					// Split column values
					String[] columnValues = line.split(COLUMN_SEPARATOR);

					// Pass line through all transformers
					for (LineTransformation lineTransformation : lineTransformations) {
						columnValues = lineTransformation.transformLine(columnValues);
					}

					// Write transformed line to temp file
					stringBuilder.setLength(0);// reuse StringBuilder
					for (int a = 0; a < columnValues.length; a++) {
						if (a > 0) {
							stringBuilder.append(COLUMN_SEPARATOR);
						}
						stringBuilder.append(columnValues[a]);
					}
					stringBuilder.append(LINE_ENDING);
					writer.write(stringBuilder.toString());
				}
			}
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

	public void addLineTransformation(LineTransformation transformation) {
		lineTransformations.add(transformation);
	}

}
