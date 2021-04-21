package org.ihtsdo.buildcloud.service.helper;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.COLUMN_SEPARATOR;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.LINE_ENDING;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.service.build.RF2Constants.RelationshipFileType;
import org.ihtsdo.buildcloud.service.build.transform.RepeatableRelationshipUUIDTransform;
import org.ihtsdo.otf.rest.exception.ProcessingException;

import com.google.common.io.Files;

public class RelationshipHelper {
	
	public static Map<String, String> buildUuidSctidMapFromPreviousRelationshipFile(String previousRelationshipFilePath,
			RelationshipFileType relFileType)
			throws ProcessingException {
		try {
			Map<String, String> uuidSctidMap = new HashMap<>();
			RepeatableRelationshipUUIDTransform relationshipUUIDTransform = new RepeatableRelationshipUUIDTransform(relFileType);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(previousRelationshipFilePath)))) {
				String line;
				String[] columnValues;
				reader.readLine(); // Discard header
				while ((line = reader.readLine()) != null) {
					columnValues = line.split(COLUMN_SEPARATOR, -1);
					uuidSctidMap.put(relationshipUUIDTransform.getCalculatedUuidFromRelationshipValues(columnValues), columnValues[0]);
				}
			}
			return uuidSctidMap;
		} catch (IOException e) {
			throw new ProcessingException("Failed to read previous relationship file during id reconciliation - "
					+ previousRelationshipFilePath, e);
		} catch (NoSuchAlgorithmException e) {
			throw new ProcessingException("Failed to use previous relationship file during id reconciliation.", e);
		}
	}

	public static Map<String, String> buildConceptToModuleIdMap(InputStream conceptSnapshotStream) throws IOException {
		Map<String, String> conceptToModuleIdMap = new HashMap<>();
		if (conceptSnapshotStream != null) {
			try (BufferedReader reader = new BufferedReader( new InputStreamReader(conceptSnapshotStream, UTF_8))) {
				//skip header
				String line = reader.readLine();
				while ((line = reader.readLine()) != null) {
					String[] splits = line.split(COLUMN_SEPARATOR);
					conceptToModuleIdMap.put(splits[0], splits[3]);
				}
			}
		} 
		return conceptToModuleIdMap;
	}
	
	public static Map<String, String> getConceptsWithModuleChange(InputStream previousSnapshotStream, Map<String, String> conceptToModuleIdMap) throws IOException {
		Map<String, String> result = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousSnapshotStream, UTF_8))) {
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] splits = line.split(COLUMN_SEPARATOR);
				if (conceptToModuleIdMap.containsKey(splits[0]) && !splits[3].equals(conceptToModuleIdMap.get(splits[0]))) {
					result.put(splits[0], conceptToModuleIdMap.get(splits[0]));
				}
			}
		}
		return result;
	}
	
	public static File generateRelationshipDeltaDueToModuleIdChange(Map<String, String> conceptToModuleMap, InputStream inferredDeltaStream,
			InputStream previousSnapshotInput, String effectiveTime) throws IOException {
		//load the relationship id from the delta into map
		Set<String> deltaRelationshipIds = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inferredDeltaStream, UTF_8))) {
			//skip header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] splits = line.split(COLUMN_SEPARATOR);
				deltaRelationshipIds.add(splits[0]);
			}
		}
		//read previous published inferred file and check for concept
		File extraInferredDelta = new File(Files.createTempDir(), "sct2_Relationship_Delta_Module_Change_Only.txt");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(previousSnapshotInput, UTF_8));
			 BufferedWriter writer = new BufferedWriter(new FileWriter(extraInferredDelta))) {
			String line = reader.readLine();
			writer.write(line);
			writer.write(LINE_ENDING);
			while ((line = reader.readLine()) != null) {
				String[] splits = line.split(COLUMN_SEPARATOR);
				if (conceptToModuleMap.containsKey(splits[4]) && !deltaRelationshipIds.contains(splits[0])) {
					line = line.replace(splits[3], conceptToModuleMap.get(splits[4]))
							.replace(splits[1], effectiveTime);
					writer.write(line);
					writer.write(LINE_ENDING);
				}
			}
		}
		return extraInferredDelta;
	}
}
