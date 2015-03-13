package org.ihtsdo.buildcloud.service.workbenchdatafix;

import org.ihtsdo.otf.rest.exception.BadInputFileException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class ModuleResolverServiceTest {

	private ModuleResolverService resolverService;
	private InputStream statedRelationshipDelta;
	private InputStream statedRelationshipSnapshot;

	@Before
	public void setUp() throws Exception {
		String testModelComponentModuleSctid = "900000000000012004";
		String testIsASctid = "116680003";
		resolverService = new ModuleResolverService(testModelComponentModuleSctid, testIsASctid);

		statedRelationshipDelta = getClass().getResourceAsStream("sct2_StatedRelationship_Delta_20150131.txt");
		statedRelationshipSnapshot = getClass().getResourceAsStream("sct2_StatedRelationship_Snapshot_20140731.txt");
	}

	@Test
	public void testGetExistingModelComponentIds() throws Exception {
		Set<String> modelComponentIds = resolverService.getExistingModelConceptIds(statedRelationshipSnapshot);

		Assert.assertEquals(3, modelComponentIds.size());
		Assert.assertTrue(modelComponentIds.contains("410660005"));
		Assert.assertTrue(modelComponentIds.contains("410662002"));
		Assert.assertTrue(modelComponentIds.contains("246501002"));
	}

	@Test
	/**
	 * Delta file includes unordered multi hop relationships
	 */
	public void testAddNewModelComponentIds() throws BadInputFileException {
		Set<String> modelComponentIds = resolverService.getExistingModelConceptIds(statedRelationshipSnapshot);
		Assert.assertEquals(3, modelComponentIds.size());
		Assert.assertTrue(modelComponentIds.contains("410660005"));
		Assert.assertTrue(modelComponentIds.contains("410662002"));
		Assert.assertTrue(modelComponentIds.contains("246501002"));

		resolverService.addNewModelConceptIds(modelComponentIds, statedRelationshipDelta);

		Assert.assertEquals(6, modelComponentIds.size());
		Assert.assertTrue(modelComponentIds.contains("410660005"));
		Assert.assertTrue(modelComponentIds.contains("410662002"));
		Assert.assertTrue(modelComponentIds.contains("246501002"));
		Assert.assertTrue(modelComponentIds.contains("246501100"));
		Assert.assertTrue(modelComponentIds.contains("246501101"));
		Assert.assertTrue(modelComponentIds.contains("246501102"));
	}

	@After
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
