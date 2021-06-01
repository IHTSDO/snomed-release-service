package org.ihtsdo.buildcloud.core.service.build.database.map;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.ihtsdo.buildcloud.core.service.build.database.RF2TableExportDAO;
import org.ihtsdo.buildcloud.core.service.build.database.map.RF2TableExportDAOImpl;
import org.ihtsdo.buildcloud.core.service.build.database.map.ReferenceSetCompositeKeyPatternFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Before;
import org.junit.Test;

public class ReferenceSetCompositeKeyPatternFactoryTest {
	
	private static final String PATTERN_WITH_FIELDS_AT_4_5_7_10 = "[^	]*	[^	]*	[^	]*	[^	]*	([^	]*	?)([^	]*	?)[^	]*	([^	]*	?)[^	]*	[^	]*	([^	]*	?).*";
	private static final String PATTERN_WITH_FIELDS_AT_4_5 = "[^	]*	[^	]*	[^	]*	[^	]*	([^	]*	?)([^	]*	?).*";
	private static final String PATTERN_WITH_FIELDS_AT_4_5_6 = "[^	]*	[^	]*	[^	]*	[^	]*	([^	]*	?)([^	]*	?)([^	]*	?).*";
	private static final String PATTERN_WITH_FIELDS_AT_4_5_6_7 = "[^	]*	[^	]*	[^	]*	[^	]*	([^	]*	?)([^	]*	?)([^	]*	?)([^	]*	?).*";
	private static final String PATTERN_WITH_FIELDS_AT_4_5_8 = "[^	]*	[^	]*	[^	]*	[^	]*	([^	]*	?)([^	]*	?)[^	]*	[^	]*	([^	]*	?).*";
	private RF2TableExportDAO dao;
	private Map<String, List<Integer>> customRefsetCompositeKeys;
	private ReferenceSetCompositeKeyPatternFactory refsetCompositeKeyPatternFactory;

	@Before
	public void setUp() throws Exception {
		customRefsetCompositeKeys = new HashMap<>();
		dao = new RF2TableExportDAOImpl(customRefsetCompositeKeys);
		refsetCompositeKeyPatternFactory = new ReferenceSetCompositeKeyPatternFactory(customRefsetCompositeKeys);
	}

	@Test
	public void testWithCustomRefsetCompositeKeysFromConfig() throws Exception {
 		customRefsetCompositeKeys.put("900000000000456007", Arrays.asList(4,5,6));
 		final String deltaInput = "der2_cciRefset_RefsetDescriptorDelta_INT_20140731.txt";
 		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
 		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000456007");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_6, pattern.toString());
	}
	
	@Test
	public void testSimpleRefset() throws Exception {
		final String deltaInput = "rel2_Refset_SimpleDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "450990004");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5, pattern.toString());
	}
	
	@Test
	public void testRefsetDescriptorDelta() throws Exception {
		final String deltaInput = "der2_cciRefset_RefsetDescriptorDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000456007");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_8, pattern.toString());
	}
	
	@Test
	public void testDescriptionType() throws Exception {
		final String deltaInput = "der2_ciRefset_DescriptionTypeDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000538005");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5, pattern.toString());
	}
	
	
	@Test
	public void testAttributeValue() throws Exception {
		final String deltaInput = "der2_cRefset_AttributeValueDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000490003");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5, pattern.toString());
	}
	
	
	@Test
	public void testLanguageRefset() throws Exception {
		final String deltaInput = "der2_cRefset_LanguageDelta-en_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000509007");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5, pattern.toString());
	}
	
	
	@Test
	public void testExtendedMapRefset() throws Exception {
		final String deltaInput = "der2_iisssccRefset_ExtendedMapDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "447562003");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_7_10, pattern.toString());
	}
	
	
	@Test
	public void testComplexMapRefset() throws Exception {
		final String deltaInput = "der2_iissscRefset_ComplexMapDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "447563008");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_7_10, pattern.toString());
	}
	
	
	@Test
	public void testSimpleMapRefset() throws Exception {
		final String deltaInput = "der2_sRefset_SimpleMapDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000497000");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_6, pattern.toString());
	}
	
	
	@Test
	public void testModuleDependencyRefset() throws Exception {
		final String deltaInput = "der2_ssRefset_ModuleDependencyDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "900000000000534007");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_6_7, pattern.toString());
	}
	
	
	@Test
	public void testAssociationReferenceRefset() throws Exception {
		final String deltaInput = "rel2_cRefset_AssociationReferenceDelta_INT_20140731.txt";
		final TableSchema tableSchema = dao.createTable(deltaInput, getClass().getResourceAsStream(deltaInput), true);
		final Pattern pattern = refsetCompositeKeyPatternFactory.getRefsetCompositeKeyPattern(tableSchema, "450990004");
		assertEquals(PATTERN_WITH_FIELDS_AT_4_5_6, pattern.toString());
	}
}
