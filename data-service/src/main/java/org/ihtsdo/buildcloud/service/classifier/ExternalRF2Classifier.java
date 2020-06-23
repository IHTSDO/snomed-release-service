package org.ihtsdo.buildcloud.service.classifier;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.COLUMN_SEPARATOR;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.DELTA;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.LINE_ENDING;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.RELASHIONSHIP_DELTA_PREFIX;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SNAPSHOT;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.STATED;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.TXT_FILE_EXTENSION;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.io.Files;

public class ExternalRF2Classifier extends RF2Classifier{

	private static final String OWL_REFSET_FILE_PATTERN = ".*_sRefset_OWL.*";

	private static final String REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA = "rel2_cissccRefset_MRCMAttributeDomainDelta";

	private static final String EQUIVALENT_CONCEPT_REFSET = "der2_sRefset_EquivalentConceptSimpleMapDelta";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired 
	private ExternalRF2ClassifierRestClient externalClassifierRestClient;
	
	@Autowired
	private BuildDAO buildDAO;
	
	@Override
	public File run(Build build, File equivalencyReportOutputFile, ClassificationInputInfo classifierFiles, File resultDir) throws BusinessServiceException {
		String previousPublished = build.getConfiguration().getPreviousPublishedPackage();
		String dependencyRelease = null;
		ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();  
		if (extensionConfig != null) {
			dependencyRelease = extensionConfig.getDependencyRelease();
			if (dependencyRelease == null || dependencyRelease.isEmpty()) {
				if (extensionConfig.isReleaseAsAnEdition()) {
					logger.warn("The product is configured as an edition without dependency package. Only previous package {} will be used in classification", previousPublished);
				} else {
					throw new BusinessServiceException("International dependency release can't be null for extension release build.");
				}
			}
		}
		File rf2DeltaZipFile = createRf2DeltaArchiveForClassifier(build, classifierFiles, resultDir);
		File inferredResult = null;
		try {
			File resultZipFile = externalClassifierRestClient.classify(rf2DeltaZipFile, previousPublished, dependencyRelease);
			File classifierResult = new File(resultDir, "result");
			if (!classifierResult.exists()) {
				classifierResult.mkdir();
			}
			if (classifierResult.exists() && classifierResult.isDirectory()) {
				ZipFileUtils.extractFilesFromZipToOneFolder(resultZipFile, classifierResult.getAbsolutePath().toString());
			} else {
				throw new BusinessServiceException("Failed to create folder to extract classification results:" + classifierResult);
			}
			for (File file : classifierResult.listFiles()) {
			  if (file.getName().endsWith(TXT_FILE_EXTENSION)) {
					if (file.getName().startsWith(RELASHIONSHIP_DELTA_PREFIX)) {
						File updatedFile = checkAndRemoveAnyExtraEmptyFields(file);
						if (updatedFile != null) {
							inferredResult = updatedFile;
						} else {
							inferredResult = file;
						}
					}
					if (file.getName().startsWith(EQUIVALENT_CONCEPT_REFSET)) {
						FileUtils.copyFile(file, equivalencyReportOutputFile);
					}
			  }
			}
			
		} catch (Exception e) {
			String errorMsg = "Error coccurred when running external classification due to:";
			if (e.getCause() != null) {
				errorMsg += e.getCause().getMessage();
			} else {
				errorMsg += e.getMessage();
			}
			throw new BusinessServiceException(errorMsg, e);
		}
		return inferredResult;
	}

	@Override
	public ClassificationInputInfo constructClassificationInputInfo(Map<String, TableSchema> inputFileSchemaMap) {
		ClassificationInputInfo pojo = new ClassificationInputInfo(true);
		for (final String inputFilename : inputFileSchemaMap.keySet()) {
			final TableSchema inputFileSchema = inputFileSchemaMap.get(inputFilename);
			if (inputFileSchema == null) {
				logger.warn("Failed to recover schema mapped to {}.", inputFilename);
				continue;
			}
			String updatedFilename = inputFileSchema.getFilename();
			if (inputFileSchema.getComponentType() == ComponentType.CONCEPT) {
				pojo.getConceptFileNames().add(updatedFilename);
			} else if (inputFileSchema.getComponentType() == ComponentType.STATED_RELATIONSHIP) {
				pojo.getStatedRelationshipFileNames().add(updatedFilename);
			} else if (inputFileSchema.getComponentType() == ComponentType.REFSET) {
				if (inputFilename.startsWith(REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA)) {
					pojo.setMrcmAttributeDomainDeltaFileName(updatedFilename);
				} else if (inputFilename.matches(OWL_REFSET_FILE_PATTERN)) {
					pojo.addOwlRefsetFile(updatedFilename);
				}
			}
		}
		return pojo;
	}
	
	private File checkAndRemoveAnyExtraEmptyFields(File rf2InferredDelta) throws FileNotFoundException, IOException {
		File updated = new File(rf2InferredDelta.getParentFile(), rf2InferredDelta.getName().replaceAll(TXT_FILE_EXTENSION, "_updated.txt"));
		boolean wrongDataFound = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(rf2InferredDelta));
			 BufferedWriter writer = new BufferedWriter(new FileWriter(updated))) {
			String line = reader.readLine();
			writer.write(line);
			writer.write(LINE_ENDING);
			int maxColumn = line.split(COLUMN_SEPARATOR).length;
			while ((line = reader.readLine()) != null) {
				String[] splits = line.split(COLUMN_SEPARATOR, -1);
				if (splits.length > maxColumn) {
					wrongDataFound = true;
					String[] updatedData = Arrays.copyOfRange(splits, 0, maxColumn);
					StringBuilder lineBuilder = new StringBuilder();
					for (int i = 0; i< updatedData.length; i++) {
						if (i > 0) {
							lineBuilder.append(COLUMN_SEPARATOR);
						}
						lineBuilder.append(updatedData[i]);
					}
					writer.write((lineBuilder.toString()));
					writer.write(LINE_ENDING);
				}
			}
		}
		if (wrongDataFound) {
			return updated;
		} else {
			return null;
		}
	}
	
	
	private File createRf2DeltaArchiveForClassifier(Build build, ClassificationInputInfo pojo, File tempDir) throws ProcessingException {
		//external classifier expects the delta files for concept, stated relationship, empty relationship and option MRCM attribute domain refset
		File rf2DeltaZipFile = new File(tempDir, "rf2Delta_" + build.getId() + ".zip");
		List<String> rf2DeltaFileList = new ArrayList<>();
		for ( String filename : pojo.getStatedRelationshipFileNames()) {
			rf2DeltaFileList.add(filename.replace(SNAPSHOT, DELTA));
		}
		
		for ( String filename : pojo.getConceptFileNames()) {
			rf2DeltaFileList.add(filename.replace(SNAPSHOT, DELTA));
		}
		
		if (pojo.getMrcmAttributeDomainDeltaFileName() != null) {
			rf2DeltaFileList.add(pojo.getMrcmAttributeDomainDeltaFileName());
		}
		
		if (pojo.getOwlRefsetFileNames() != null) {
			rf2DeltaFileList.addAll(pojo.getOwlRefsetFileNames());
		}
		
		logger.info("rf2 delta files prepared for external classification:" + rf2DeltaFileList.toString());
	
		File deltaTempDir = Files.createTempDir();
		downloadFiles(build, deltaTempDir, rf2DeltaFileList, buildDAO);
		//add empty relationship delta as the external classifier currently requires it to be present
		String inferredDeltaFile = null;
		try {
			inferredDeltaFile = createEmptyRelationshipDelta(deltaTempDir);
		} catch (IOException e) {
			throw new ProcessingException("Error occured when creating empty relaitonship delta file.", e);
		}
		if (inferredDeltaFile == null) {
			throw new ProcessingException("Failed to create empty relaitonship delta file.");
		}
		try {
			ZipFileUtils.zip(deltaTempDir.getAbsolutePath(), rf2DeltaZipFile.getAbsolutePath());
		} catch (IOException e) {
			throw new ProcessingException("Failed to zip RF2 delta files.", e);
		}
		return rf2DeltaZipFile;
		
	}

	private String createEmptyRelationshipDelta(File deltaTempDir) throws IOException {
		String inferredDelta = null;
		for (File file : deltaTempDir.listFiles() ) {
			if (file.getName().endsWith(TXT_FILE_EXTENSION) && file.getName().contains(STATED)) {
				inferredDelta = file.getName().replace(ComponentType.STATED_RELATIONSHIP.toString(), ComponentType.RELATIONSHIP.toString());
				File statedFile = new File(deltaTempDir, file.getName());
				File inferredDeltaFile = new File(deltaTempDir, inferredDelta);
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(inferredDeltaFile));
					 BufferedReader reader = new BufferedReader(new FileReader(statedFile))) {
					String header = reader.readLine();
					writer.write(header);
					writer.write(LINE_ENDING);
				} 
				break;
			}
		}
		return inferredDelta;
	}
}
