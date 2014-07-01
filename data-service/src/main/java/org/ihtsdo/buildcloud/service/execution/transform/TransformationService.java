package org.ihtsdo.buildcloud.service.execution.transform;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.service.exception.EffectiveDateNotMatchedException;
import org.ihtsdo.buildcloud.service.execution.CachedSctidFactory;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.database.FileRecognitionException;
import org.ihtsdo.buildcloud.service.execution.database.SchemaFactory;
import org.ihtsdo.buildcloud.service.execution.database.TableType;
import org.ihtsdo.idgeneration.IdAssignmentBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TransformationService {

	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private IdAssignmentBI idAssignmentBI;

	@Autowired
	private ExecutionDAO dao;

	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationService.class);

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 * @param execution
	 */
	public void transformFiles(Execution execution, org.ihtsdo.buildcloud.entity.Package pkg) {
		String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveTimeSnomedFormat();
		String releaseId = effectiveDateInSnomedFormat;
		String executionId = execution.getId();
		CachedSctidFactory cachedSctidFactory = new CachedSctidFactory(null, releaseId, executionId, idAssignmentBI);
		TransformationFactory transformationFactory = new TransformationFactory(effectiveDateInSnomedFormat, cachedSctidFactory);

		String packageBusinessKey = pkg.getBusinessKey();
		LOGGER.info("Transforming files in execution {}, package {}", execution.getId(), packageBusinessKey);

		// Iterate each execution input file
		List<String> executionInputFilePaths = dao.listInputFilePaths(execution, packageBusinessKey);

		// Recognise table type
		Map<String, TableType> inputFilesMap = getFileTableTypes(effectiveDateInSnomedFormat, executionInputFilePaths);

		// Phase 1
		// Process just the id column of any Concept files.
		for (String relativeFilePath : inputFilesMap.keySet()) {
			TableType tableType = inputFilesMap.get(relativeFilePath);
			if (tableType == TableType.CONCEPT) {
				try {
					InputStream executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, relativeFilePath);
					OutputStream transformedOutputStream = dao.getLocalTransformedFileOutputStream(execution, packageBusinessKey, relativeFilePath);

					StreamingFileTransformation steamingFileTransformation = transformationFactory.getPreProcessConceptFileTransformation();

					// Apply transformations
					steamingFileTransformation.transformFile(executionInputFileInputStream, transformedOutputStream);

				} catch (TransformationException | IOException e) {
					// Catch blocks just log and let the next file get processed.
					LOGGER.error("Exception occurred when transforming file {}", relativeFilePath, e);
				}
			}
		}

		// Phase 2
		// Process all files
		for (String relativeFilePath : executionInputFilePaths) {
			// Transform all txt files
			if (relativeFilePath.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
				TableType tableType = inputFilesMap.get(relativeFilePath);
				try {
					InputStream executionInputFileInputStream;
					if (tableType == TableType.CONCEPT) {
						executionInputFileInputStream = dao.getLocalInputFileStream(execution, packageBusinessKey, relativeFilePath);
					} else {
						executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, relativeFilePath);
					}

					AsyncPipedStreamBean asyncPipedStreamBean = dao.getTransformedFileOutputStream(execution, packageBusinessKey, relativeFilePath);
					OutputStream executionTransformedOutputStream = asyncPipedStreamBean.getOutputStream();

					// Get appropriate transformations for this file type.
					StreamingFileTransformation steamingFileTransformation = transformationFactory.getSteamingFileTransformation(tableType);

					// Apply transformations
					steamingFileTransformation.transformFile(executionInputFileInputStream, executionTransformedOutputStream);

					// Wait for upload of transformed file to finish
					asyncPipedStreamBean.waitForFinish();

				} catch (FileRecognitionException e) {
					LOGGER.error("Did not recognise input file '{}'.", relativeFilePath, e);
				} catch (TransformationException | IOException e) {
					// Catch blocks just log and let the next file get processed.
					LOGGER.error("Exception occurred when transforming file {}", relativeFilePath, e);
				} catch (ExecutionException | InterruptedException e) {
					LOGGER.error("Exception occurred when uploading transformed file {}", relativeFilePath, e);
				}
			} else {
				dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
			}
		}
	}

	/**
	 * @param fileName input text file name.
	 * @param effectiveDate  date in format of "yyyyMMdd"
	 */
	private void checkFileHasGotMatchingEffectiveDate(String fileName, String effectiveDate) {
		String[] segments = fileName.split(RF2Constants.FILE_NAME_SEPARATOR);
		//last segment will be like 20140131.txt
		String dateFromFile = segments[segments.length - 1].substring(0, effectiveDate.length());
		if(!dateFromFile.equals(effectiveDate)){
			throw new EffectiveDateNotMatchedException("Effective date from build:" + effectiveDate + " does not match the date from input file:" + fileName);
		}
	}

	private Map<String, TableType> getFileTableTypes(String effectiveDateInSnomedFormat, List<String> executionInputFilePaths) {
		Map<String, TableType> inputFilesMap = new HashMap<>();
		for (String relativeFilePath : executionInputFilePaths) {
			// Transform all txt files
			if (relativeFilePath.endsWith(RF2Constants.TXT_FILE_EXTENSION)) {
				checkFileHasGotMatchingEffectiveDate(relativeFilePath, effectiveDateInSnomedFormat);
				try {
					String fileName = relativeFilePath.substring(relativeFilePath.lastIndexOf("/") + 1);
					TableType tableType = schemaFactory.getTableType(fileName);
					inputFilesMap.put(relativeFilePath, tableType);
				} catch (FileRecognitionException e) {
					LOGGER.error("Did not recognise input file '{}'.", relativeFilePath, e);
				}
			}
		}
		return inputFilesMap;
	}

}
