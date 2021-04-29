package org.ihtsdo.buildcloud.service.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.buildcloud.config.TestConfig;
import org.ihtsdo.buildcloud.dao.*;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.*;
import org.ihtsdo.buildcloud.service.build.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
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

	//OWL files
	public static final String INPUT_OWL_EXPRESSION_FILE_NAME = "rel2_sRefset_OWLExpressionDelta_INT_20190131.txt";
	public static final String EXPECTED_OWL_EXPRESSION_DELTA_FILE_NAME = "xsct2_sRefset_OWLExpressionDelta_INT_20190131.txt";
	public static final String EXPECTED_OWL_EXPRESSION_SNAPSHOT_FILE_NAME = "xsct2_sRefset_OWLExpressionFull_INT_20190131.txt";
	public static final String EXPECTED_OWL_EXPRESSION_FULL_FILE_NAME = "xsct2_sRefset_OWLExpressionSnapshot_INT_20190131.txt";
	public static final String PREVIOUS_OWL_AXIOM_FULL_FILE_NAME = "sct2_sRefset_OWLAxiomFull_INT_20180731.txt";
	public static final String PREVIOUS_OWL_AXIOM_SNAPSHOT_FILE_NAME = "sct2_sRefset_OWLAxiomSnapshot_INT_20180731.txt";
	public static final String PREVIOUS_OWL_ONTHOLOGY_FULL_FILE_NAME = "sct2_sRefset_OWLOntologyFull_INT_20180731.txt";
	public static final String PREVIOUS_OWL_ONTHOLOGY_SNAPSHOT_FILE_NAME = "sct2_sRefset_OWLOntologySnapshot_INT_20180731.txt";
	public static final String PREVIOUS_OWL_AXIOM_DELTA_FILE_NAME = "sct2_sRefset_OWLAxiomDelta_INT_20180731.txt";
	public static final String PREVIOUS_OWL_ONTHOLOGY_DELTA_FILE_NAME = "sct2_sRefset_OWLOntologyDelta_INT_20180731.txt";

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

		final Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(build, dao, 1);
		rf2ExportService.generateReleaseFiles();

		final List<String> outputFiles = dao.listOutputFilePaths(build);
		Assert.assertEquals(3, outputFiles.size());
		
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_DELTA_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_DELTA_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_FULL_FILE_NAME), dao.getOutputFileInputStream(build, EXPECTED_SIMPLE_FULL_FILE_NAME));

	}

	@Test
	public void testGenerateNewFileIncludingPrevReleaseFiles() throws Exception {
		buildConfiguration.setFirstTimeRelease(true);
		buildConfiguration.setPreviousPublishedPackage("20180731");
		buildConfiguration.setBetaRelease(true);
		buildConfiguration.setEffectiveTimeFormatted("2019-01-31");
		String path = "int/20180731/";
		Map<String, Set<String>> includedFileMap = new HashMap<>();
		Set<String> includedFiles = new HashSet<>();
		includedFiles.add(PREVIOUS_OWL_AXIOM_DELTA_FILE_NAME);
		includedFiles.add(PREVIOUS_OWL_ONTHOLOGY_DELTA_FILE_NAME);
		includedFileMap.put(INPUT_OWL_EXPRESSION_FILE_NAME,includedFiles);
		buildConfiguration.setIncludePrevReleaseFiles(INPUT_OWL_EXPRESSION_FILE_NAME + "(" + PREVIOUS_OWL_AXIOM_DELTA_FILE_NAME + "," + PREVIOUS_OWL_ONTHOLOGY_DELTA_FILE_NAME + ")");
		s3Client.putObject(BUILD_BUCKET_NAME, transformedFileFullPath + EXPECTED_OWL_EXPRESSION_DELTA_FILE_NAME, getFileByName(EXPECTED_OWL_EXPRESSION_DELTA_FILE_NAME));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, path + PREVIOUS_OWL_AXIOM_FULL_FILE_NAME, getFileByName(PREVIOUS_OWL_AXIOM_FULL_FILE_NAME));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, path + PREVIOUS_OWL_AXIOM_SNAPSHOT_FILE_NAME, getFileByName(PREVIOUS_OWL_AXIOM_SNAPSHOT_FILE_NAME));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, path + PREVIOUS_OWL_ONTHOLOGY_FULL_FILE_NAME, getFileByName(PREVIOUS_OWL_ONTHOLOGY_FULL_FILE_NAME));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, path + PREVIOUS_OWL_ONTHOLOGY_SNAPSHOT_FILE_NAME, getFileByName(PREVIOUS_OWL_ONTHOLOGY_SNAPSHOT_FILE_NAME));

		final Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(build, dao, 1);
		rf2ExportService.generateReleaseFiles();

		final List<String> outputFiles = dao.listOutputFilePaths(build);
		Assert.assertEquals(3, outputFiles.size());
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_OWL_EXPRESSION_DELTA_FILE_NAME), dao.getOutputFileInputStream(build,EXPECTED_OWL_EXPRESSION_DELTA_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_OWL_EXPRESSION_SNAPSHOT_FILE_NAME), dao.getOutputFileInputStream(build,EXPECTED_OWL_EXPRESSION_SNAPSHOT_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_OWL_EXPRESSION_FULL_FILE_NAME), dao.getOutputFileInputStream(build,EXPECTED_OWL_EXPRESSION_FULL_FILE_NAME));

	}

	@Test
	public void testEmptyValueIdFix() throws Exception {
		buildConfiguration.setFirstTimeRelease(false);
		buildConfiguration.setPreviousPublishedPackage(PREVIOUS_RELEASE);
		buildConfiguration.setWorkbenchDataFixesRequired(true);
		s3Client.putObject(BUILD_BUCKET_NAME, transformedFileFullPath + TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE, getFileByName(TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_FULL_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_FULL_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE));

		final Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(build, dao, 1);
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
