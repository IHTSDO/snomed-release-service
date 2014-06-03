package org.ihtsdo.buildcloud.packageprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class FileTransformation {

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String LINE_ENDING = "\r\n";

	private List<LineTransformation> lineTransformations;
	private static final String COLUMN_SEPARATOR = "\t";

	public FileTransformation() {
		lineTransformations = new ArrayList<>();
	}

	public void transformFile(File inputFile) throws IOException {

		// Temp file to receive results stream
		Path tempFile = Files.createTempFile(getClass().getName(), inputFile.getName());

		try (BufferedReader reader = Files.newBufferedReader(inputFile.toPath(), UTF_8);
			 BufferedWriter writer = Files.newBufferedWriter(tempFile, UTF_8)) {

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

		}

		// Replace input file with transformed temp file
		Files.move(tempFile, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

	}

	public void addLineTransformation(LineTransformation transformation) {
		lineTransformations.add(transformation);
	}

}
