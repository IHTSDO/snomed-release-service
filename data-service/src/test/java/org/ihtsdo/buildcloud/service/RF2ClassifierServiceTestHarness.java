package org.ihtsdo.buildcloud.service;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.snomed.util.rf2.schema.ComponentType;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
/*
 * Add local config before run the harness 
 * eg: -DdataServicePropertiesPath=file:///Users/mchu/srs/config/config.properties
 */
public class RF2ClassifierServiceTestHarness {
	@Autowired
	private RF2ClassifierService classifierService;
	
	@Test
	public void testClassifierWrapper() throws BusinessServiceException, ParseException {
		Build build = createBuild();
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		inputFileSchemaMap.put("rel2_StatedRelationship_Snapshot_US1000124_20170301.txt", new TableSchema(ComponentType.STATED_RELATIONSHIP, "sct2_StatedRelationship_Snapshot_US1000124_20170301"));
		inputFileSchemaMap.put("rel2_Concept_Snapshot_US1000124_20170301.txt", new TableSchema(ComponentType.CONCEPT, "sct2_Concept_Snapshot_US1000124_20170301"));
		classifierService.generateInferredRelationshipSnapshot(build, inputFileSchemaMap);
	}

	private Build createBuild() throws ParseException {
		Date releaseDate = DateUtils.parseDate("20170301", "yyyyMMdd");
		Product testProduct = new Product("snomed_ct_us_edition_20170301_testing");
		testProduct.setReleaseCenter(new ReleaseCenter("US release center", "us"));
		Build build = new Build(releaseDate, testProduct);
		BuildConfiguration configuration = new BuildConfiguration();
		configuration.setBetaRelease(false);
		configuration.setCreateLegacyIds(false);
		configuration.setEffectiveTime(releaseDate);
		ExtensionConfig extensionConfig = new ExtensionConfig();
		extensionConfig.setModuleId("731000124108");
		extensionConfig.setNamespaceId("1000124");
		extensionConfig.setReleaseAsAnEdition(true);
		extensionConfig.setDependencyRelease("SnomedCT_InternationalRF2_Production_20170131T120000.zip");
		configuration.setExtensionConfig(extensionConfig);
		configuration.setFirstTimeRelease(false);
		configuration.setPreviousPublishedPackage("SnomedCT_RF2Release_US1000124_20160901.zip");
		build.setConfiguration(configuration);
		return build;
	}

}
