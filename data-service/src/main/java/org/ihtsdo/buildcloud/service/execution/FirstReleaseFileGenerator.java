package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

import java.util.List;

public class FirstReleaseFileGenerator extends ReleaseFileGenerator{
	
	public FirstReleaseFileGenerator(Execution execution, Package pkg, ExecutionDAO dao) {
		super(execution, pkg, dao);
	}

	/**
	 * Generate full,snapshot and delta files.
	 */
	@Override
	public void generateReleaseFiles() {
		generateDeltaFiles(true);
		convertDeltaFilesTo(RF2Constants.FULL);
		convertDeltaFilesTo(RF2Constants.SNAPSHOT);
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
	private void convertDeltaFilesTo(String fileType) {

		String businessKey = pkg.getBusinessKey();
		List<String> transformedFileNames = executionDao.listTransformedFilePaths(execution, businessKey);
		
		if (transformedFileNames.size() < 1) {
			throw new ReleaseFileGenerationException("Failed to find any transformed files to convert to " + fileType);
		}
		int filesProcessed = 0;
		for (String fileName : transformedFileNames) {
			if (fileName.endsWith(RF2Constants.TXT_FILE_EXTENSION)
					&& fileName.contains(RF2Constants.DELTA)) {
				executionDao.copyTransformedFileToOutput(execution,
						businessKey, fileName,
						fileName.replace(RF2Constants.DELTA, fileType));
				filesProcessed++;
			}
		}
		if (filesProcessed < 1) {
			throw new ReleaseFileGenerationException("Failed to find any files of type *Delta*.txt to convert to " + fileType + ".");
		}
	}

	
}
