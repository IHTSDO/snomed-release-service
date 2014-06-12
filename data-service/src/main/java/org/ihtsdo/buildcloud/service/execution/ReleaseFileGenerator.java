package org.ihtsdo.buildcloud.service.execution;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

public class ReleaseFileGenerator {

	private static final String TXT_FILE_EXTENSION = ".txt";
	private static final String DELTA = "Delta";
	private static final String FULL = "Full";
	private static final String SNAPSHOT = "Snapshot";
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String LINE_ENDING = "\r\n";
	private ExecutionDAO dao;
	private Execution execution;
	
	private Package pkg;

	public ReleaseFileGenerator(Execution execution, Package pkg, ExecutionDAO dao) {
		this.execution = execution;
		this.dao = dao;
		this.pkg = pkg;
	}

	/**
	 * Generate full,snapshot and delta files.
	 * @throws Exception 
	 */
	public void generateReleaseFiles() throws Exception {
		boolean isFirstRelease = execution.getBuild().isFirstTimeRelease();
		generateFullFiles(isFirstRelease);
		generateSnapshotFiles(isFirstRelease);
		generateDeltaFiles(isFirstRelease);

	}

	private void generateFullFiles(boolean isFirstRelease) throws Exception {

		// first release just rename the delta file to full

		if (isFirstRelease) {
			convertDeltaFilesTo(FULL);
		} else {
			// generate full files combining previous release and delta files
			throw new UnsupportedOperationException("Sorry we only support first time release at the moment!");

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
	 * @throws Exception 
	 */
	private void convertDeltaFilesTo(String fileType) throws Exception {

		String businessKey = pkg.getBusinessKey();
		List<String> transformedFilePaths = dao.listTransformedFilePaths(execution, businessKey);
		
		if (transformedFilePaths.size() < 1) {
			throw new Exception ("Failed to find any transformed files to convert to " + fileType);
		}
		int filesProcessed = 0;
		for (String fileName : transformedFilePaths) {
			if (fileName.endsWith(TXT_FILE_EXTENSION)
					&& fileName.contains(DELTA)) {
				dao.copyTransformedFileToOutput(execution,
						businessKey, fileName,
						fileName.replace(DELTA, fileType));
				filesProcessed++;
			}
		}
		if (filesProcessed < 1) {
			throw new Exception ("Failed to find any files of type *Delta*.txt to convert to " + fileType + ".");
		}
	}

	private void generateDeltaFiles(boolean isFirstRelease) throws Exception {

		String businessKey = pkg.getBusinessKey();
		List<String> transformedFilePaths = dao.listTransformedFilePaths(execution, businessKey);
		
		if (transformedFilePaths.size() < 1) {
			throw new Exception ("Failed to find any transformed files to convert to output delta files.");
		}
		
		int filesProcessed = 0;		
		for (String fileName : transformedFilePaths) {
			if (fileName.endsWith(TXT_FILE_EXTENSION)
					&& fileName.contains(DELTA)) {
				if (isFirstRelease) {
					// remove delta file contents and only keep the header
					// line
					InputStream inputStream = dao
							.getTransformedFileAsInputStream(execution,
									businessKey, fileName);
					OutputStream outputStream = dao
							.getOutputFileOutputStream(execution,
									businessKey, fileName);
					try (	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
							BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8))) {
						// only needs to read the first line.
						String line = reader.readLine();
						if (line == null) {
							throw new IllegalStateException( "No contents found in file: " + fileName);
						}
						writer.write(line);
						writer.write(LINE_ENDING);
					}
				} else {
					//In subsequent releases, the delta file received as input should be the actual change from the previous release.  Copy as is.
					dao.copyTransformedFileToOutput(execution,
							businessKey, fileName);
				}
				filesProcessed++;
			}
			
			if (filesProcessed < 1) {
				throw new Exception ("Failed to find any files of type *Delta*.txt to convert to output delta files.");
			}
		}
	}

	private void generateSnapshotFiles(boolean isFirstRelease) throws Exception {

		if (isFirstRelease) {
			convertDeltaFilesTo(SNAPSHOT);
		} else {
			// generate snapshot files using delta file and previous release.
			throw new UnsupportedOperationException("Sorry we only support first time release at the moment!");
		}

	}

}
