package org.ihtsdo.buildcloud.service.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test/testDataServiceContext.xml" })
public class Rf2FileExportRunnerTest {

	private static final String PREVIOUS_RELEASE = "previousRelease";
	private static final String PUBLISHED_BUCKET_NAME = "local.published.bucket";
	private static final String BUILD_BUCKET_NAME = "local.build.bucket";
	private static final String RELEASE_DATE = "20140731";

	// Simple refset
	private static final String TRANSFORMED_SIMPLE_DELTA_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140731.txt";
	private static final String EXPECTED_SIMPLE_FULL_FILE_NAME = "der2_Refset_SimpleFull_INT_20140731.txt";
	private static final String EXPECTED_SIMPLE_DELTA_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140731.txt";
	private static final String EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME = "der2_Refset_SimpleSnapshot_INT_20140731.txt";

	// Attribute value refset
	private static final String TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE = "der2_cRefset_AttributeValueDelta_INT_20140731.txt";
	private static final String PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE = "der2_cRefset_AttributeValueSnapshot_INT_20140331.txt";
	private static final String PREVIOUS_ATTRIBUT_VALUE_FULL_FILE = "der2_cRefset_AttributeValueFull_INT_20140331.txt";
	private static final String EXPECTED_ATTRIBUT_VALUE_DELTA_FILE = "der2_cRefset_AttributeValueDelta_INT_20140731.txt";
	private static final String EXPECTED_ATTRIBUT_VALUE_SNAPSHOT_FILE = "der2_cRefset_AttributeValueSnapshot_INT_20140731.txt";
	private static final String EXPECTED_ATTRIBUT_VALUE_FULL_FILE = "der2_cRefset_AttributeValueFull_INT_20140731.txt";
	
	private static final String LANGUAGE_REFSET = "der2_cRefset_LanguageDelta-en_INT_20150731_transformed.txt";
	private Product product;
	@Autowired
	private BuildDAO dao;
	@Autowired
	private S3Client s3Client;
	private String transformedFileFullPath;
	private String publishedPath;
	private final PesudoUUIDGenerator uuidGenerator = new PesudoUUIDGenerator();
	private Build build;
	private BuildConfiguration buildConfiguration;

	@Before
	public void setUp() throws IOException {
		product = new Product("Test");
		final ReleaseCenter releaseCenter = new ReleaseCenter("INTERNATIONAL", "INT");
		product.setReleaseCenter(releaseCenter);
		final Date date = new Date();
		buildConfiguration = product.getBuildConfiguration();
		if (buildConfiguration == null) {
			buildConfiguration = new BuildConfiguration();
			product.setBuildConfiguration(buildConfiguration);
		}
		build = new Build(date, product);
		final SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
		try {
			buildConfiguration.setEffectiveTime(formater.parse(RELEASE_DATE));
		} catch (final ParseException e) {
			throw new IllegalArgumentException("Release date format is not valid:" + RELEASE_DATE, e);
		}
		transformedFileFullPath = "int/test/" + EntityHelper.formatAsIsoDateTime(date) + "/transformed-files/";
		publishedPath = "int/" + PREVIOUS_RELEASE + "/";
	}
	
	@Test
	public void testGenerateFirstReleaseForSimpleRefset() throws Exception {
		buildConfiguration.setFirstTimeRelease(true);
		buildConfiguration.setWorkbenchDataFixesRequired(false);
		s3Client.putObject(BUILD_BUCKET_NAME, transformedFileFullPath + TRANSFORMED_SIMPLE_DELTA_FILE_NAME, getFileByName(TRANSFORMED_SIMPLE_DELTA_FILE_NAME));

		final Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(build, dao, uuidGenerator, 1);
		rf2ExportService.generateReleaseFiles();

		final List<String> outputFiles = dao.listOutputFilePaths(build);
		Assert.assertEquals(3, outputFiles.size());
		
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_DELTA_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_DELTA_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_FULL_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_FULL_FILE_NAME));

	}

	@Test
	public void testEmptyValueIdFix() throws Exception {
		buildConfiguration.setFirstTimeRelease(false);
		buildConfiguration.setPreviousPublishedPackage(PREVIOUS_RELEASE);
		buildConfiguration.setWorkbenchDataFixesRequired(true);
		s3Client.putObject(BUILD_BUCKET_NAME, transformedFileFullPath + TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE, getFileByName(TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_FULL_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_FULL_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE));

		final Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(build, dao, uuidGenerator, 1);
		rf2ExportService.generateReleaseFiles();

		final List<String> outputFiles = dao.listOutputFilePaths(build);
		Assert.assertEquals(3, outputFiles.size());
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_DELTA_FILE), dao.getOutputFileInputStream(build, EXPECTED_ATTRIBUT_VALUE_DELTA_FILE));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_SNAPSHOT_FILE), dao.getOutputFileInputStream(build, EXPECTED_ATTRIBUT_VALUE_SNAPSHOT_FILE));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_FULL_FILE), dao.getOutputFileInputStream(build, EXPECTED_ATTRIBUT_VALUE_FULL_FILE));
	}

	private InputStream getExpectedFileInputStreamFromResource(final String fileName) throws FileNotFoundException {
		final String filePath = getClass().getResource("/org/ihtsdo/buildcloud/service/build/export/expected/" + fileName).getFile();
		return new FileInputStream(filePath);
	}

	private File getFileByName(final String fileName) {
		return new File(getClass().getResource("/org/ihtsdo/buildcloud/service/build/export/" + fileName).getFile());
	}
	
	@After
	public void tearDown() throws InterruptedException {
		uuidGenerator.reset();
		Thread.sleep(1000); // Delay to prevent build id overlap
	}
}
