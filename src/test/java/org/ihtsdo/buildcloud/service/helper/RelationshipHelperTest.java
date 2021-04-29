package org.ihtsdo.buildcloud.service.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class RelationshipHelperTest {
	
	private final Map<String, String> conceptToModuleMap = new HashMap<>();
	@Before
	public void setUp() {
		conceptToModuleMap.put("741232009","900000000000207008");
		conceptToModuleMap.put("741232010","900000000000207008");
	}
	
	@Test
	public void testInferedDeltaDueToConceptModuleIdChange() throws IOException, BusinessServiceException {
		InputStream inferredDeltaStream = RelationshipHelperTest.class.getResourceAsStream("sct2_Relationship_Delta_INT_20180131.txt");
		InputStream previousInferredSnapshotStream = RelationshipHelperTest.class.getResourceAsStream("sct2_Relationship_Snapshot_INT_20170731.txt");
		File extraDelta = RelationshipHelper.generateRelationshipDeltaDueToModuleIdChange(conceptToModuleMap, 
				inferredDeltaStream, previousInferredSnapshotStream , "20180131");
		assertNotNull(extraDelta);
		assertEquals("sct2_Relationship_Delta_Module_Change_Only.txt", extraDelta.getName());
		assertTrue(extraDelta.exists());
		List<String> result = readLinesFromFile(extraDelta);
		assertEquals(2, result.size());
		String expected = "7346485023	20180131	1	900000000000207008	741232009	123027009	0	704323007	900000000000010007	900000000000451002";
		assertEquals(expected, result.get(1));
	}
	
	@Test
	public void testGetConceptsWithModuleChange() throws IOException {
		InputStream previousSnapshotStream = RelationshipHelperTest.class.getResourceAsStream("sct2_Concept_Snapshot_INT_20170731.txt");
		Map<String, String> result = RelationshipHelper.getConceptsWithModuleChange(previousSnapshotStream, conceptToModuleMap);
		assertEquals(1, result.size());
		assertEquals("900000000000207008",result.get("741232010"));
	}
	
	private List<String> readLinesFromFile( File fileToRead) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(fileToRead))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}
}
