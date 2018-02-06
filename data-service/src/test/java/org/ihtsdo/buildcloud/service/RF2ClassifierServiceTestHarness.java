package org.ihtsdo.buildcloud.service;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.classifier.ClassificationResult;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.Files;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class RF2ClassifierServiceTestHarness {
	private static final String RELATIONSHIP_HEADER_LINE = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
	private static final String CONCEPT_HEADER_LINE = "id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId";
	@Autowired
	private RF2ClassifierService classifierService;
	@Autowired 
	private BuildDAO buildDao;
	@Autowired
	private PublishService publishService;
	ReleaseCenter internationalCenter;
	ReleaseCenter usReleaseCenter;
	
	@Before
	public void setUp() {
		//Add {Root_Dir}/snomed-release-service/data-service/release
		internationalCenter = new ReleaseCenter("International Release Center", "international");
		usReleaseCenter = new ReleaseCenter("US release center", "us");
	}
	
	
	@Test
	public void testInternalClassifierForExtensionWithEmptyData() throws Exception {
		File previousPublished = new File("release/SnomedCT_ExtensionRF2_Test_20170901T120000Z.zip");
		assertTrue(previousPublished.exists());
		File dependencyRelease = new File("release/SnomedCT_InternationalRF2_Test_20180131T120000Z.zip");
		assertTrue(dependencyRelease.exists());
	
		Build build = createEditionReleaseBuild(false, previousPublished, dependencyRelease, false);
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Snapshot_Extension_20180301.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Snapshot_Extension_20180301"));
		
		inputFileSchemaMap.put("rel2_Concept_Snapshot_Extension_20180301.txt",
				new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Snapshot_Extension_20180301"));
		addFilesToBuild(build, "sct2_StatedRelationship_Snapshot_Extension_20180301.txt","sct2_Concept_Snapshot_Extension_20180301.txt");
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		System.out.println(result);
		assertNotNull(result);
		assertTrue(result.isSnapshot());
	}
	
	@Test
	public void testInternalClassifierForExtension() throws Exception {
		File previousPublished = new File("release/SnomedCT_USExtensionRF2_PRODUCTION_20170901T120000Z.zip");
		assertTrue(previousPublished.exists());
		File dependencyRelease = new File("release/SnomedCT_InternationalRF2_PRODUCTION_20180131T120000Z.zip");
		assertTrue(dependencyRelease.exists());
	
		Build build = createEditionReleaseBuild(false, previousPublished, dependencyRelease, false);
		build.getConfiguration().setBetaRelease(true);
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Snapshot_US1000124_20180301.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "xsct2_StatedRelationship_Snapshot_US1000124_20180301"));
		
		inputFileSchemaMap.put("rel2_Concept_Snapshot_US1000124_20180301.txt",
				new TableSchema(ComponentType.CONCEPT, "xsct2_Concept_Snapshot_US1000124_20180301"));
		addFilesToBuild(build, "release/xsct2_StatedRelationship_Snapshot_US1000124_20180301.txt","release/xsct2_Concept_Snapshot_US1000124_20180301.txt");
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		System.out.println(result);
		assertNotNull(result);
		assertTrue(result.isSnapshot());
		assertEquals("xsct2_Relationship_Snapshot_US1000124_20180301.txt", result.getResultFilename());
	}


	@Test
	public void testInternalClassifierWithUsEdition() throws Exception {
		File previousPublished = new File("release/SnomedCT_USEditionRF2_PRODUCTION_20170901T120000Z.zip");
		assertTrue(previousPublished.exists());
		File dependencyRelease = new File("release/SnomedCT_InternationalRF2_PRODUCTION_20180131T120000Z.zip");
		assertTrue(dependencyRelease.exists());
	
		Build build = createEditionReleaseBuild(true, previousPublished, dependencyRelease, false);
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Snapshot_US1000124_20180301.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Snapshot_US1000124_20180301"));
		
		inputFileSchemaMap.put("rel2_Concept_Snapshot_US1000124_20180301.txt",
				new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Snapshot_US1000124_20180301"));
		addFilesToBuild(build, "release/sct2_StatedRelationship_Snapshot_US1000124_20180301.txt","release/sct2_Concept_Snapshot_US1000124_20180301.txt");
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		assertNotNull(result);
		assertTrue(result.isSnapshot());
		assertEquals("sct2_Relationship_Snapshot_US1000124_20180301.txt", result.getResultFilename());
	}

	@Test
	public void testExternalClassification() throws Exception {
		File previousPublished = new File("release/SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z.zip");
		assertTrue(previousPublished.exists());
		Build build = createInternationalBuild("20180131", "classification_test", previousPublished, true);
		build.getConfiguration().setBetaRelease(true);
		String rootDir = "release/CS_integration_test/";
		prepareTestFiles(build, rootDir + "xsct2_Concept_Delta_INT_20180131.txt", 
				rootDir + "xsct2_StatedRelationship_Delta_INT_20180131.txt",
				rootDir + "xsct2_StatedRelationship_Snapshot_INT_20180131.txt",
				rootDir + "xsct2_Concept_Snapshot_INT_20180131.txt",
				rootDir + "xsct2_Relationship_Delta_INT_20180131.txt",
				rootDir + "xder2_cissccRefset_MRCMAttributeDomainDelta_INT_20180131.txt",
				rootDir + "xder2_sRefset_OWLAxiomReferenceSetDelta_INT_20180131.txt");
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Delta_INT_20180131.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Delta_INT_20180131"));
		
		inputFileSchemaMap.put("rel2_Concept_Delta_INT_20180131.txt",
				new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Delta_INT_20180131"));
		
		inputFileSchemaMap.put("rel2_cissccRefset_MRCMAttributeDomainDelta_INT_20180131.txt",
				new TableSchema(ComponentType.REFSET, "der2_cissccRefset_MRCMAttributeDomainDelta_INT_20180131.txt"));
		
		inputFileSchemaMap.put("rel2_sRefset_OWLAxiomReferenceSetDelta_INT_20180131.txt",
				new TableSchema(ComponentType.REFSET, "der2_sRefset_OWLAxiomReferenceSetDelta_INT_20180131.txt"));
		
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		assertNotNull(result);
		assertTrue(!result.isSnapshot());
		System.out.println(result.getResultFilename());
		assertEquals("xsct2_Relationship_Delta_INT_20180131.txt", result.getResultFilename());
	}
	
	private void prepareTestFiles(Build build, String ... filenames) throws IOException {
		for (String filename : filenames) {
			buildDao.putOutputFile(build, new File(filename), false);
		}
	}

	private Build createInternationalBuild(String effectiveTime, String productName, File previousPublished, boolean useExternalClassifier) throws Exception {
		Date releaseDate = DateUtils.parseDate(effectiveTime, "yyyyMMdd");
		Product testProduct = new Product(productName);
		testProduct.setReleaseCenter(internationalCenter);
		if (!publishService.exists(internationalCenter, previousPublished.getName())) {
			publishService.publishAdHocFile(internationalCenter, new FileInputStream(previousPublished),
					previousPublished.getName(), previousPublished.length(), false);
		}
		
		Build build = new Build(releaseDate, testProduct);
		BuildConfiguration configuration = new BuildConfiguration();
		configuration.setBetaRelease(false);
		configuration.setCreateLegacyIds(false);
		configuration.setEffectiveTime(releaseDate);
		configuration.setFirstTimeRelease(false);
		configuration.setUseExternalClassifier(useExternalClassifier);
		configuration.setPreviousPublishedPackage(previousPublished.getName());
		build.setConfiguration(configuration);
		return build;
	}
	
	private Build createEditionReleaseBuild(boolean isEditionRelease, File previousPublished, File internationalDependnecy, boolean useExternalClassifier) throws Exception {
		Date releaseDate = DateUtils.parseDate("20170301", "yyyyMMdd");
		Product testProduct = new Product("snomed_ct_us_edition_20170301_testing");
		testProduct.setReleaseCenter(usReleaseCenter);
		if (!publishService.exists(usReleaseCenter, previousPublished.getName())) {
			publishService.publishAdHocFile(usReleaseCenter, new FileInputStream(previousPublished),
					previousPublished.getName(), previousPublished.length(), false);
		}
	
		if (!publishService.exists(internationalCenter, internationalDependnecy.getName())) {
			publishService.publishAdHocFile(internationalCenter, new FileInputStream(internationalDependnecy),
					internationalDependnecy.getName(), internationalDependnecy.length(), false);
		}
		
		Build build = new Build(releaseDate, testProduct);
		BuildConfiguration configuration = new BuildConfiguration();
		configuration.setBetaRelease(false);
		configuration.setCreateLegacyIds(false);
		configuration.setEffectiveTime(releaseDate);
		ExtensionConfig extensionConfig = new ExtensionConfig();
		extensionConfig.setModuleId("731000124108");
		extensionConfig.setNamespaceId("1000124");
		extensionConfig.setReleaseAsAnEdition(isEditionRelease);
		extensionConfig.setDependencyRelease(internationalDependnecy.getName());
		configuration.setExtensionConfig(extensionConfig);
		configuration.setFirstTimeRelease(false);
		configuration.setPreviousPublishedPackage(previousPublished.getName());
		configuration.setUseExternalClassifier(useExternalClassifier);
		build.setConfiguration(configuration);
		return build;
	}
	
	private void addFilesToBuild(Build build, String ...filenames) throws IOException {
		File tempDir = Files.createTempDir();
		for (String filename : filenames) {
			File outputFile = null;
			if (!filename.startsWith("release/") && !filename.startsWith("/")) {
				//create empty temp file
				outputFile = createEmptyTempFile(filename, tempDir);
			} else {
				outputFile = new File(filename);
			}
			buildDao.putOutputFile(build, outputFile);
		}
	}
	
	private File createEmptyTempFile(String filename, File tempDir) throws IOException {
		String headerLine = null;
		if (filename.contains("Concept")) {
			headerLine = CONCEPT_HEADER_LINE;
		} else if (filename.contains("Relationship")) {
			headerLine = RELATIONSHIP_HEADER_LINE;
		} 
		File temp = null;
		if (headerLine != null) {
			temp = new File(tempDir, filename);
			try( BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
				writer.write(headerLine);
				writer.newLine();
			}
		}
		return temp;
	}
}
