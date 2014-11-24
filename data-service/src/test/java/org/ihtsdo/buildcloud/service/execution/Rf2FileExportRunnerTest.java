package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.s3.S3Client;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.service.execution.transform.PesudoUUIDGenerator;
import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test/testDataServiceContext.xml" })
public class Rf2FileExportRunnerTest {

	private static final String PREVIOUS_RELEASE = "previousRelease";
	private static final String PUBLISHED_BUCKET_NAME = "local.published.bucket";
	private static final String EXECUTION_BUCKET_NAME = "local.execution.bucket";
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
	
	private Product product;
	@Autowired
	private ExecutionDAO dao;
	@Autowired
	private S3Client s3Client;
	private String transformedFileFullPath;
	private String publishedPath;
	private final PesudoUUIDGenerator uuidGenerator = new PesudoUUIDGenerator();
	private Execution execution;

	@Before
	public void setUp() throws IOException {
		product = new Product(1L, "Test");
		ReleaseCenter releaseCenter = new ReleaseCenter("INTERNATIONAL", "INT");
		product.setReleaseCenter(releaseCenter);
		Date date = new Date();
		execution = new Execution(date, product);
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
		try {
			product.setEffectiveTime(formater.parse(RELEASE_DATE));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Release date format is not valid:" + RELEASE_DATE, e);
		}
		transformedFileFullPath = "int/test/" + EntityHelper.formatAsIsoDateTime(date) + "/transformed-files/";
		publishedPath = "int/" + PREVIOUS_RELEASE + "/";
	}

	@Test
	public void testGenerateFirstReleaseForSimpleRefset() throws Exception {
		product.setFirstTimeRelease(true);
		product.setWorkbenchDataFixesRequired(false);
		s3Client.putObject(EXECUTION_BUCKET_NAME, transformedFileFullPath + TRANSFORMED_SIMPLE_DELTA_FILE_NAME, getFileByName(TRANSFORMED_SIMPLE_DELTA_FILE_NAME));

		Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(execution, dao, uuidGenerator, 1);
		rf2ExportService.generateReleaseFiles();

		List<String> outputFiles = dao.listOutputFilePaths(execution);
		Assert.assertEquals(3, outputFiles.size());
		
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_DELTA_FILE_NAME), dao.getOutputFileInputStream(execution, EXPECTED_SIMPLE_DELTA_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME), dao.getOutputFileInputStream(execution, EXPECTED_SIMPLE_SNAPSHOT_FILE_NAME));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_SIMPLE_FULL_FILE_NAME), dao.getOutputFileInputStream(execution, EXPECTED_SIMPLE_FULL_FILE_NAME));

	}

	@Test
	public void testEmptyValueIdFix() throws Exception {
		product.setFirstTimeRelease(false);
		product.setPreviousPublishedPackage(PREVIOUS_RELEASE);
		product.setWorkbenchDataFixesRequired(true);
		s3Client.putObject(EXECUTION_BUCKET_NAME, transformedFileFullPath + TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE, getFileByName(TRANSFORMED_ATTRIBUT_VALUE_DELTA_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_FULL_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_FULL_FILE));
		s3Client.putObject(PUBLISHED_BUCKET_NAME, publishedPath + PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE, getFileByName(PREVIOUS_ATTRIBUT_VALUE_SNAPSHOT_FILE));

		Rf2FileExportRunner rf2ExportService = new Rf2FileExportRunner(execution, dao, uuidGenerator, 1);
		rf2ExportService.generateReleaseFiles();

		List<String> outputFiles = dao.listOutputFilePaths(execution);
		Assert.assertEquals(3, outputFiles.size());
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_DELTA_FILE), dao.getOutputFileInputStream(execution, EXPECTED_ATTRIBUT_VALUE_DELTA_FILE));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_SNAPSHOT_FILE), dao.getOutputFileInputStream(execution, EXPECTED_ATTRIBUT_VALUE_SNAPSHOT_FILE));
		StreamTestUtils.assertStreamsEqualLineByLine(getExpectedFileInputStreamFromResource(EXPECTED_ATTRIBUT_VALUE_FULL_FILE), dao.getOutputFileInputStream(execution, EXPECTED_ATTRIBUT_VALUE_FULL_FILE));
	}

	@Test
	public void testExportFullAndDeltaFromSnapshotAndPrevFull() {

	}

	private InputStream getExpectedFileInputStreamFromResource(String fileName) throws FileNotFoundException {
		String filePath = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/export/expected/" + fileName).getFile();
		return new FileInputStream(filePath);
	}

	private File getFileByName(String fileName) {
		return new File(getClass().getResource("/org/ihtsdo/buildcloud/service/execution/export/" + fileName).getFile());

	}
	
	@After
	public void tearDown() throws InterruptedException {
		uuidGenerator.reset();
		Thread.sleep(1000); // Delay to prevent execution id overlap
	}
}
