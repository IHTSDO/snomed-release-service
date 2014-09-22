package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.service.exception.ProcessingException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.classifier.CycleCheck;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class RF2ClassifierService {

	@Autowired
	private ExecutionDAO executionDAO;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public boolean checkNoStatedRelationshipCycles(Execution execution, Package pkg, Map<String, TableSchema> inputFileSchemaMap) throws ProcessingException {
		String packageBusinessKey = pkg.getBusinessKey();
		List<String> conceptSnapshotFilenames = new ArrayList<>();
		List<String> statedRelationshipSnapshotFilenames = new ArrayList<>();

		for (String inputFilename : inputFileSchemaMap.keySet()) {
			TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);
			if (inputFileSchema.getComponentType().equals(ComponentType.CONCEPT)) {
				conceptSnapshotFilenames.add(inputFilename.replace(SchemaFactory.REL_2, SchemaFactory.SCT_2).replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT));
			} else if (inputFileSchema.getComponentType().equals(ComponentType.STATED_RELATIONSHIP)) {
				statedRelationshipSnapshotFilenames.add(inputFilename.replace(SchemaFactory.REL_2, SchemaFactory.SCT_2).replace(RF2Constants.DELTA, RF2Constants.SNAPSHOT));
			}
		}

		if (!conceptSnapshotFilenames.isEmpty() && !statedRelationshipSnapshotFilenames.isEmpty()) {
			File tempDir = Files.createTempDir();

			// Download snapshot files
			List<String> localConceptFilePaths = downloadFiles(execution, packageBusinessKey, tempDir, conceptSnapshotFilenames);
			List<String> localStatedRelationshipFilePaths = downloadFiles(execution, packageBusinessKey, tempDir, statedRelationshipSnapshotFilenames);

			File cycleFile = new File(tempDir, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
			try {
				CycleCheck cycleCheck = new CycleCheck(localConceptFilePaths, localStatedRelationshipFilePaths, cycleFile.getAbsolutePath());
				boolean cycleDetected = cycleCheck.cycleDetected();
				if (cycleDetected) {
					// Upload cycles file
					try (FileInputStream in = new FileInputStream(cycleFile)) {
						AsyncPipedStreamBean logFileOutputStream = executionDAO.getLogFileOutputStream(execution, packageBusinessKey, RF2Constants.CONCEPTS_WITH_CYCLES_TXT);
						StreamUtils.copy(in, logFileOutputStream.getOutputStream());
						logFileOutputStream.waitForFinish();
					} catch (ExecutionException | InterruptedException e) {
						logger.error("Failed to upload relationship cycle file", e);
					}
				}
				return !cycleDetected;
			} catch (IOException e) {
				throw new ProcessingException("IO problem during stated relationship cycle check.", e);
			}
		} else {
			logger.info("Stated relationship and concept files not present. Skipping this step.");
			return true;
		}
	}

	public List<String> downloadFiles(Execution execution, String packageBusinessKey, File tempDir, List<String> filenameLists) throws ProcessingException {
		List<String> localFilePaths = new ArrayList<>();
		for (String downloadFilename : filenameLists) {

			File localFile = new File(tempDir, downloadFilename);
			try (InputStream inputFileStream = executionDAO.getOutputFileInputStream(execution, packageBusinessKey, downloadFilename);
				 FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} else {
					throw new ProcessingException("Didn't find output file " + downloadFilename);
				}
			} catch (IOException e) {
				logger.error("Failed to download snapshot file for classifier cycle check.", e);
				// TODO: throw something
			}
		}
		return localFilePaths;
	}
}
