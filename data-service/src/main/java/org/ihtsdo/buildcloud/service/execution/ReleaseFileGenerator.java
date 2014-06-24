package org.ihtsdo.buildcloud.service.execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;

/**
 * Abstract class for ReleaseFileGenerator.
 * 
 */
public abstract class ReleaseFileGenerator {
	final Execution execution;
	final Package pkg;
	final Product product;
	final ExecutionDAO executionDao;

	/**
	 * Constructor.
	 * @param executionX
	 *            An Execution
	 * @param pkgX
	 *            A package.
	 */
	public ReleaseFileGenerator(final Execution executionX, final Package pkgX, final ExecutionDAO dao) {
		execution = executionX;
		pkg = pkgX;
		product = pkg.getBuild().getProduct();
		executionDao = dao;
	}

	/**
	 * Generate full,snapshot and delta files.
	 */
	public abstract void generateReleaseFiles();

	/**
	 * @return the transformed delta file name exception if not found.
	 */
	protected String getTransformedDeltaFile() {
		String businessKey = pkg.getBusinessKey();
		List<String> transformedFilePaths = executionDao.listTransformedFilePaths(
				execution, businessKey);
		if (transformedFilePaths.size() < 1) {
			throw new RuntimeException(
					"Failed to find any transformed files to convert to output delta files.");
		}
		String deltaFileName = null;
		for (String fileName : transformedFilePaths) {
			if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)
					&& fileName.contains(RF2Constants.DELTA)) {
				deltaFileName = fileName;
				//expects to be only one
				break;
			}
		}
		if (deltaFileName == null) {
			throw new ReleaseFileGenerationException(
					"Failed to find any files of type *Delta*.txt transformed in package:"
							+ businessKey);
		}
		return deltaFileName;
	}

	/**
	 * Generate Delta Release File.
	 * @param deltaFileName
	 *            Current transformed delta file name
	 * @param isFirstRelease
	 *            true if it is the first time release
	 */
	protected final void generateDeltaFiles(final String deltaFileName,
			final boolean isFirstRelease) {
		String businessKey = pkg.getBusinessKey();
		if (isFirstRelease) {
			try {
				// remove delta file contents and only keep the header line
				InputStream inputStream = executionDao
						.getTransformedFileAsInputStream(
						execution, businessKey, deltaFileName);
				AsyncPipedStreamBean asyncPipedStreamBean;

				asyncPipedStreamBean = executionDao.getOutputFileOutputStream(
						execution,
						businessKey, deltaFileName);
				OutputStream outputStream = asyncPipedStreamBean
						.getOutputStream();
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, RF2Constants.UTF_8));
						BufferedWriter writer = new BufferedWriter(
								new OutputStreamWriter(outputStream,
										RF2Constants.UTF_8))) {
					// only needs to read the first line.
					String line = reader.readLine();
					if (line == null) {
						throw new IllegalStateException(
								"No contents found in file: " + deltaFileName);
					}
					writer.write(line);
					writer.write(RF2Constants.LINE_ENDING);
				}
				// Wait for upload pipe to finish
				asyncPipedStreamBean.waitForFinish();
			} catch (IOException | ExecutionException | InterruptedException e) {
				throw new ReleaseFileGenerationException(
						"Failed to generate first release delta file!", e);
			}
		} else {
			// In subsequent releases, the delta file received as input should
			// be the actual change from the previous release. Copy as is.
			executionDao.copyTransformedFileToOutput(execution, businessKey,
					deltaFileName);
		}
	}
}
