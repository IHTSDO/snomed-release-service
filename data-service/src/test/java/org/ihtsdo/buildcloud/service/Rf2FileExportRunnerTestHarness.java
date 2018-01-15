package org.ihtsdo.buildcloud.service;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.service.build.ReleaseFileGenerationException;
import org.ihtsdo.buildcloud.service.build.Rf2FileExportRunner;
import org.ihtsdo.buildcloud.service.build.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.classifier.ClassificationResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test/testDataServiceContext.xml"})
/*
 * Add local config before run the harness 
 * eg: -DdataServicePropertiesPath=file:///Users/mchu/srs/config/config.properties -Xms1024m -Xmx4g
 */
public class Rf2FileExportRunnerTestHarness {
	@Autowired
	private BuildDAO dao;
	@Autowired
	private UUIDGenerator uuidGenerator;
	
	@Test
	public void testEditionReleaseExport() throws ParseException, ReleaseFileGenerationException {
		Build build = createBuild();
		Rf2FileExportRunner exportRunner = new Rf2FileExportRunner(build,dao, 1);
		String transformedSnapshotFilename = "sct2_Relationship_Snapshot_US1000124_20170301.txt";
		exportRunner.generateRelationshipFiles(new ClassificationResult(transformedSnapshotFilename, true));
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
