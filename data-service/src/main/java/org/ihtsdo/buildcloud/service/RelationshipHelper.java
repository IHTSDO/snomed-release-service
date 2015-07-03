package org.ihtsdo.buildcloud.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.build.transform.RepeatableRelationshipUUIDTransform;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.springframework.beans.factory.annotation.Autowired;

@Resource
public class RelationshipHelper {
	
	@Autowired
	private BuildDAO buildDAO;
	
	private static final String STATED_RELATIONSHIP = "_StatedRelationship_";

	public static Map<String, String> buildUuidSctidMapFromPreviousRelationshipFile(String previousRelationshipFilePath,
			RF2Constants.RelationshipFileType relFileType)
			throws ProcessingException {
		try {
			Map<String, String> uuidSctidMap = new HashMap<>();
			RepeatableRelationshipUUIDTransform relationshipUUIDTransform = new RepeatableRelationshipUUIDTransform(relFileType);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(previousRelationshipFilePath)))) {
				String line;
				String[] columnValues;
				reader.readLine(); // Discard header
				while ((line = reader.readLine()) != null) {
					columnValues = line.split(RF2Constants.COLUMN_SEPARATOR, -1);
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
	
	public String getStatedRelationshipInputFile(Build build) {
		
		//get a list of input file names
		final List<String> inputfilesList = buildDAO.listInputFileNames(build);
		for (final String inputFileName : inputfilesList) { 
			if (inputFileName.contains(STATED_RELATIONSHIP)) {
				return inputFileName;
			}
		}
			
		return null;
	}
}
