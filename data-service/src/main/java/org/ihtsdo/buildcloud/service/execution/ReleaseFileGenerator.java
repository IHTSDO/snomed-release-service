package org.ihtsdo.buildcloud.service.execution;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.FileService;
import org.ihtsdo.buildcloud.entity.Package;

public class ReleaseFileGenerator {

	private static final String TXT_FILE_EXTENSION = ".txt";
	private static final String DELTA = "Delta";
	private static final String FULL = "Full";
	private static final String SNAPSHOT = "Snapshot";
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String LINE_ENDING = "\r\n";
	private FileService fileService;
	private Execution execution;

	public ReleaseFileGenerator(Execution executionX, FileService fileServiceX) {
		execution = executionX;
		fileService = fileServiceX;
	}

	/**
	 * Generate full,snapshot and delta files.
	 * 
	 * @throws IOException
	 */
	public void generateReleaseFiles() throws IOException {
		boolean isFirstRelease = execution.getBuild().isFirstTimeRelease();
		generateFullFiles(isFirstRelease);
		generateSnapshotFiles(isFirstRelease);
		generateDeltaFiles(isFirstRelease);

	}

	private void generateFullFiles(boolean isFirstRelease) {

		// first release just rename the delta file to full

		if (isFirstRelease) {
			convertDeltaFilesTo(FULL);
		} else {
			// generate full files combining previous release and delta files

		}
	}

	/**
	 * It simply copies the delta files from transformed folder to output folder
	 * and changes the file name by replacing the delta to file type (i.e Full
	 * or Snapshot). Example: der2_Refset_SimpleDelta_INT_20140831.txt ---->
	 * der2_Refset_SimpleFull_INT_20140831.txt
	 * 
	 * 
	 * @param fileType
	 *            the type of file to be converted to.
	 */
	private void convertDeltaFilesTo(String fileType) {

		for (Package pk : execution.getBuild().getPackages()) {
			String businessKey = pk.getBusinessKey();
			for (String fileName : fileService.listTransformedFilePaths(
					execution, businessKey)) {
				if (fileName.endsWith(TXT_FILE_EXTENSION)
						&& fileName.contains(DELTA)) {
					fileService.copyTransformedFileToOutput(execution,
							businessKey, fileName,
							fileName.replace(DELTA, fileType));
				}
			}
		}
	}

	private void generateDeltaFiles(boolean isFirstRelease) throws IOException {

		for (Package pk : execution.getBuild().getPackages()) {
			String businessKey = pk.getBusinessKey();
			for (String fileName : fileService.listTransformedFilePaths(
					execution, businessKey)) {
				if (fileName.endsWith(TXT_FILE_EXTENSION)
						&& fileName.contains(DELTA)) {
					if (isFirstRelease) {
						// remove delta file contents and only keep the header
						// line
						InputStream inputStream = fileService
								.getTransformedFileAsInputStream(execution,
										businessKey, fileName);
						OutputStream outputStream = fileService
								.getExecutionOutputFileOutputStream(execution,
										businessKey, fileName);
						try (BufferedReader reader = new BufferedReader(
								new InputStreamReader(inputStream, UTF_8));
								BufferedWriter writer = new BufferedWriter(
										new OutputStreamWriter(outputStream,
												UTF_8))) {
							// only needs to read the first line.
							String line = reader.readLine();
							if (line == null) {
								throw new IllegalStateException(
										"No contents found in file: "
												+ fileName);
							}
							writer.write(line);
							writer.write(LINE_ENDING);
						}
					} else {

						fileService.copyTransformedFileToOutput(execution,
								businessKey, fileName);
					}
				}
			}
		}
	}

	private void generateSnapshotFiles(boolean isFirstRelease) {

		if (isFirstRelease) {

			convertDeltaFilesTo(SNAPSHOT);
		} else {

			// generate snapshot files using delta file and previous release.
		}

	}

}
