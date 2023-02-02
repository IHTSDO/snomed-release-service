package org.ihtsdo.buildcloud.core.service.workbenchdatafix;

import org.ihtsdo.otf.rest.exception.BadInputFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleResolverServiceTest {

	private ModuleResolverService resolverService;
	private InputStream statedRelationshipDelta;
	private InputStream statedRelationshipSnapshot;

	@BeforeEach
	public void setUp() throws Exception {
		String testModelComponentModuleSctid = "900000000000012004";
		String testIsASctid = "116680003";
		resolverService = new ModuleResolverService();

		statedRelationshipDelta = getClass().getResourceAsStream("sct2_StatedRelationship_Delta_20150131.txt");
		statedRelationshipSnapshot = getClass().getResourceAsStream("sct2_StatedRelationship_Snapshot_20140731.txt");
	}

	@Test
	public void testGetExistingModelComponentIds() throws Exception {
		Set<String> modelComponentIds = resolverService.getExistingModelConceptIds(statedRelationshipSnapshot);

		assertEquals(3, modelComponentIds.size());
		assertTrue(modelComponentIds.contains("410660005"));
		assertTrue(modelComponentIds.contains("410662002"));
		assertTrue(modelComponentIds.contains("246501002"));
	}

	@Test
	/**
	 * Delta file includes unordered multi hop relationships
	 */
	public void testAddNewModelComponentIds() throws BadInputFileException {
		Set<String> modelComponentIds = resolverService.getExistingModelConceptIds(statedRelationshipSnapshot);
		assertEquals(3, modelComponentIds.size());
		assertTrue(modelComponentIds.contains("410660005"));
		assertTrue(modelComponentIds.contains("410662002"));
		assertTrue(modelComponentIds.contains("246501002"));

		resolverService.addNewModelConceptIds(modelComponentIds, statedRelationshipDelta);

		assertEquals(6, modelComponentIds.size());
		assertTrue(modelComponentIds.contains("410660005"));
		assertTrue(modelComponentIds.contains("410662002"));
		assertTrue(modelComponentIds.contains("246501002"));
		assertTrue(modelComponentIds.contains("246501100"));
		assertTrue(modelComponentIds.contains("246501101"));
		assertTrue(modelComponentIds.contains("246501102"));
	}

	@AfterEach
	public void tearDown() {
		try {
			statedRelationshipDelta.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			statedRelationshipSnapshot.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
