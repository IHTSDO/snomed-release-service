package org.ihtsdo.buildcloud.service;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.hamcrest.CoreMatchers;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
public class RF2ClassificationServiceTest {

	@Autowired 
	private RF2ClassifierService classifierService;
	
	private ReleaseCenter internationalCenter;
	
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();
	
	@Before
	public void setUp() {
		internationalCenter = new ReleaseCenter("International Release Center", "international");
	}
	
	@Test
	public void testClassificationWithoutSufficientData() throws Exception {
		expectedEx.expect(BusinessServiceException.class);
		expectedEx.expectMessage(CoreMatchers.equalTo("Classification can't be run due to stated relationship and concept files are missing"));
		Build build = createBuild("test.zip");
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		classifierService.classify(build, inputFileSchemaMap);
	}
	
	@Test
	public void testExternalClassificationWithNoFileFound() throws Exception{
		expectedEx.expect(ProcessingException.class);
		expectedEx.expectMessage(CoreMatchers.equalTo("Didn't find output file:xsct2_StatedRelationship_Delta_INT_20180301.txt"));
		Build build = createBuild("test.zip");
		build.getConfiguration().setBetaRelease(true);
		Map<String, TableSchema> inputFileSchemaMap = createInputFilesSchemaMap();
		classifierService.classify(build, inputFileSchemaMap);
	}

	private Build createBuild(String previousPublished) throws ParseException {
		Date releaseDate = DateUtils.parseDate("20180731", "yyyyMMdd");
		Product testProduct = new Product("Test");
		testProduct.setReleaseCenter(internationalCenter);
		Build build = new Build(releaseDate, testProduct);
		BuildConfiguration configuration = new BuildConfiguration();
		configuration.setBetaRelease(false);
		configuration.setCreateLegacyIds(false);
		configuration.setEffectiveTime(releaseDate);
		configuration.setFirstTimeRelease(false);
		configuration.setUseExternalClassifier(true);
		configuration.setPreviousPublishedPackage(previousPublished);
		build.setConfiguration(configuration);
		return build;
	}
	
	private Map<String, TableSchema> createInputFilesSchemaMap() {
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Delta_INT_20180301.txt", 
				new TableSchema(ComponentType.STATED_RELATIONSHIP, "xsct2_StatedRelationship_Delta_INT_20180301"));
		
		inputFileSchemaMap.put("rel2_Concept_Delta_INT_20180301.txt",
				new TableSchema(ComponentType.CONCEPT, "xsct2_Concept_Delta_INT_20180301"));
		return inputFileSchemaMap;
	}
}
