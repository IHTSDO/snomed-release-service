package org.ihtsdo.buildcloud.service.execution;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.test.DummyFuture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RunWith(JMockit.class)
public class Rf2FileExportServiceTest {

	private static final String DELTA_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";
	private static final String FuLL_FILE_NAME = "der2_Refset_SimpleFull_INT_20140731.txt";

	@Mocked
	private Build build;

	@Mocked
	private Product product;

	@Test
	public void testGenerateFirstReleaseFiles(@Injectable final Execution execution, @Injectable final ExecutionDAO dao) throws Exception {

		final List<Package> packages = createPackages(true);
		String deltaFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/" + DELTA_FILE_NAME).getFile();
		File outputDeltaFile = Files.createTempFile(getClass().getCanonicalName(), DELTA_FILE_NAME).toFile();
		build.setProduct(new Product("Test Product"));

		new NonStrictExpectations() {{
			execution.getBuild();
			returns(build);
			build.getPackages();
			returns(packages);
			build.getProduct();
			returns(product);
			build.getEffectiveTime();
			SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
			returns(formater.parse("20130831"));
		}};

		final List<String> fileNames = mockTransformedFileNames();
		final String currentFullFile = getCurrentReleaseFile(RF2Constants.FULL);
		final String currentSnapshotFile = getCurrentReleaseFile(RF2Constants.SNAPSHOT);

		final OutputStream deltaOutputStream = new FileOutputStream(outputDeltaFile);
		final AsyncPipedStreamBean deltaAsyncPipedStreamBean = new AsyncPipedStreamBean(deltaOutputStream, new DummyFuture());

		new Expectations() {{
			dao.listTransformedFilePaths(withInstanceOf(Execution.class), anyString);
			returns(fileNames);

			dao.getTransformedFileAsInputStream(withInstanceOf(Execution.class), anyString, anyString);
			returns(getFileInputStreamFromResource(DELTA_FILE_NAME));

			dao.getOutputFileOutputStream(execution, anyString, "der2_Refset_SimpleDelta_INT_20140831.txt");
			returns(deltaAsyncPipedStreamBean);
			dao.getOutputFileOutputStream(execution, anyString, anyString);
			returns(getDummyAsyncPipedStreamBean(currentFullFile));
			dao.getOutputFileOutputStream(execution, anyString, anyString);
			returns(getDummyAsyncPipedStreamBean(currentSnapshotFile));
		}};

		int maxRetries = 0;
		for (Package pkg : packages) {
			Rf2FileExportService generator = new Rf2FileExportService(execution, pkg, dao, maxRetries);
			generator.generateReleaseFiles();
		}

		//get the original header line from delta file
		String headerLine = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(deltaFile), RF2Constants.UTF_8))) {

			headerLine = reader.readLine();
		}

		//check and make sure the output delta file has only header line
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputDeltaFile), RF2Constants.UTF_8))) {
			String actualHeader = reader.readLine();
			if ((reader.readLine()) != null) {
				Assert.fail("It should not contain more than one line!");
			}
			Assert.assertEquals("Header lines should match", actualHeader, headerLine);
		}

		//add assert to check files are generated
		File fullFile = new File(currentFullFile);
		File snapshotFile = new File(currentSnapshotFile);
		Assert.assertTrue("Full file should be generated", fullFile.exists());
		Assert.assertTrue("Snapshort file should be generated", snapshotFile.exists());
	}

	@Test
	public void testGenerateFilesForSubsequentRelease(@Injectable final Execution execution,
													  @Injectable final ExecutionDAO dao) throws Exception {
		final List<Package> packages = createPackages(false);
		new NonStrictExpectations() {{
			execution.getBuild();
			returns(build);
			build.getPackages();
			returns(packages);
			build.getProduct();
			returns(product);
			build.getEffectiveTime();
			SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
			returns(formater.parse("20130831"));
		}};

		final List<String> fileNames = mockTransformedFileNames();
		final String currentFullFile = getCurrentReleaseFile(RF2Constants.FULL);
		final String currentSnapshotFile = getCurrentReleaseFile(RF2Constants.SNAPSHOT);

		new Expectations() {{
			dao.listTransformedFilePaths(withInstanceOf(Execution.class), anyString);
			returns(fileNames);

			dao.getTransformedFileAsInputStream(withInstanceOf(Execution.class), anyString, anyString);
			returns(getFileInputStreamFromResource(DELTA_FILE_NAME));

			dao.getOutputFileOutputStream(execution, anyString, anyString);
			returns(getDummyAsyncPipedStreamBean(currentFullFile));

			dao.getPublishedFileArchiveEntry(product, anyString, anyString);
			returns(getFileInputStreamFromResource(FuLL_FILE_NAME));

			dao.getOutputFileOutputStream(execution, anyString, anyString);
			returns(getDummyAsyncPipedStreamBean(currentFullFile));
			dao.getOutputFileOutputStream(execution, anyString, anyString);
			returns(getDummyAsyncPipedStreamBean(currentSnapshotFile));
		}};

		int maxRetries = 0;
		for (Package pkg : packages) {
			Rf2FileExportService generator = new Rf2FileExportService(execution, pkg, dao, maxRetries);
			generator.generateReleaseFiles();
		}

		//add assert to check files are generated
		File fullFile = new File(currentFullFile);
		File snapshotFile = new File(currentSnapshotFile);
		Assert.assertTrue("Full file should be generated", fullFile.exists());
		Assert.assertTrue("Snapshort file should be generated", snapshotFile.exists());
	}

	private List<String> mockTransformedFileNames() {
		final List<String> fileNames = new ArrayList<>();
		fileNames.add(DELTA_FILE_NAME);
		return fileNames;
	}

	private List<Package> createPackages(boolean isFirstRelease) {
		List<Package> packages = new ArrayList<>();
		Package pk = new Package("PK1");
		pk.setBuild(build);
		pk.setFirstTimeRelease(isFirstRelease);
		packages.add(pk);
		return packages;
	}

	private InputStream getFileInputStreamFromResource(String fileName) throws FileNotFoundException {
		String filePath = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/" + fileName).getFile();
		return new FileInputStream(filePath);
	}

	private AsyncPipedStreamBean getDummyAsyncPipedStreamBean(String fileName) throws FileNotFoundException {
		final OutputStream outputStream = new FileOutputStream(fileName);
		return new AsyncPipedStreamBean(outputStream, new DummyFuture());
	}

	private String getCurrentReleaseFile(String fileType) {
		String deltaFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/" + DELTA_FILE_NAME).getFile();
		return deltaFile.replace(RF2Constants.DELTA, fileType);
	}

}
