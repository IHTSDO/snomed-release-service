package org.ihtsdo.buildcloud.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
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
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class RF2ClassifierServiceTestHarness {
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
		internationalCenter = new ReleaseCenter("International Release Center", "international");
		usReleaseCenter = new ReleaseCenter("US release center", "us");
	}
	
	@Test
	public void testInternalClassifierWithUsEdition() throws BusinessServiceException, ParseException, FileNotFoundException {
		File previousPublished = new File("release/SnomedCT_InternationalRF2_Production_20170131T120000.zip");
		assertTrue(previousPublished.exists());
		File dependencyRelease = new File("release/SnomedCT_InternationalRF2_Production_20170131T120000.zip");
		assertTrue(dependencyRelease.exists());
	
		Build build = createEditionReleaseBuild(previousPublished, dependencyRelease);
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Snapshot_US1000124_20170301.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Snapshot_US1000124_20170301"));
		
		inputFileSchemaMap.put("rel2_Concept_Snapshot_US1000124_20170301.txt",
				new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Snapshot_US1000124_20170301"));
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		assertNotNull(result);
		assertTrue(result.isSnapshot());
	}

	@Test
	public void testExternalClassification() throws Exception {
		File previousPublished = new File("release/SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z.zip");
		assertTrue(previousPublished.exists());
		Build build = createInternationalBuild("20180131", "classification_test", previousPublished);
		String rootDir = "release/CS_integration_test/";
		prepareTestFiles(build, rootDir + "sct2_Concept_Delta_INT_20180131.txt", 
				rootDir + "sct2_StatedRelationship_Delta_INT_20180131.txt",
				rootDir + "sct2_StatedRelationship_Snapshot_INT_20180131.txt",
				rootDir + "sct2_Concept_Snapshot_INT_20180131.txt",
				rootDir + "stc2_Relationship_Delta_INT_20180131.txt");
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Delta_INT_20180131.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Delta_INT_20180131"));
		
		inputFileSchemaMap.put("rel2_Concept_Delta_INT_20180131.txt",
				new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Delta_INT_20180131"));
		ClassificationResult result = classifierService.classify(build, inputFileSchemaMap);
		assertNotNull(result);
		assertTrue(!result.isSnapshot());
		System.out.println(result.getResultFilename());
	}
	
	private void prepareTestFiles(Build build, String ... filenames) throws IOException {
		for (String filename : filenames) {
			buildDao.putOutputFile(build, new File(filename), false);
		}
	}

	private Build createInternationalBuild(String effectiveTime, String productName, File previousPublished) throws ParseException, FileNotFoundException, BusinessServiceException {
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
		configuration.setUseExternalClassifier(true);
		configuration.setPreviousPublishedPackage(previousPublished.getName());
		build.setConfiguration(configuration);
		return build;
	}
	
	private Build createEditionReleaseBuild(File previousPublished, File internationalDependnecy) throws ParseException, FileNotFoundException, BusinessServiceException {
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
		extensionConfig.setReleaseAsAnEdition(true);
		extensionConfig.setDependencyRelease(internationalDependnecy.getName());
		configuration.setExtensionConfig(extensionConfig);
		configuration.setFirstTimeRelease(false);
		configuration.setPreviousPublishedPackage(previousPublished.getName());
		build.setConfiguration(configuration);
		return build;
	}
}
