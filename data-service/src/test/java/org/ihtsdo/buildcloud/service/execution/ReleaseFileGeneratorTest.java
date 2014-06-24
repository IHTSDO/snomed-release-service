package org.ihtsdo.buildcloud.service.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JMockit.class)
public class ReleaseFileGeneratorTest {

	protected static final String DELTA_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";
	@Mocked Build build;
	
	@Test
	public void testGenerateFirstReleaseFiles(@Injectable final Execution execution, @Injectable final ExecutionDAO dao) throws Exception
	{
		final List<Package> packages = createPackages( true );
		final List<String> fileNames = mockTransformedFileNames();
		
		build.setProduct(new Product("Test Product"));
		
		new NonStrictExpectations() {{
			execution.getBuild();
			returns( build);
			returns(true);
			execution.getBuild();
			returns( build);
			build.getPackages();
			returns(packages);
			dao.listTransformedFilePaths( withInstanceOf(Execution.class),anyString );
			returns(fileNames);
		}};
		
		new NonStrictExpectations(2) { {
			
			for( Package pk : packages )
			{
				for( String deltaName : fileNames ){
					
					dao.copyTransformedFileToOutput(execution, pk.getBusinessKey(), deltaName, anyString);
				}
			}
			
		} };
		
		String deltaFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ DELTA_FILE_NAME).getFile();
		final InputStream inputStream = new FileInputStream(deltaFile);
	
		String outputDeltaFile = deltaFile.replace(".txt", "_output.txt");
		final OutputStream outputStream = new FileOutputStream(outputDeltaFile);
		final AsyncPipedStreamBean asyncPipedStreamBean = new AsyncPipedStreamBean(outputStream, new DummyFuture());
		new Expectations() {{
			for (Package pkg : packages) {
				for (String fileName : fileNames) {
					dao.getTransformedFileAsInputStream(execution, pkg.getBusinessKey(), fileName);
					returns(inputStream);
					dao.getOutputFileOutputStream(execution, pkg.getBusinessKey(), fileName);
					returns(asyncPipedStreamBean);
				}
			}
		}};
		
		for (Package pkg : packages) {
			ReleaseFileGenerator generator = new FirstReleaseFileGenerator(execution, pkg, dao);
			generator.generateReleaseFiles();
		}
		
		//get the original header line from delta file
		String headerLine = null;
		try( BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream( deltaFile ))) ) {
			
			headerLine = reader.readLine();
		}
			
		//check and make sure the output delta file has only header line
		try( BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream( outputDeltaFile ))) ) {
			
			String actualHeader = reader.readLine();
			if( ( reader.readLine() ) != null )
			{
				fail("It should not contain more than one line!");
			}
			assertEquals("Header lines should match",actualHeader, headerLine );
		}
	}
	
	@Test
	@Ignore
	//TODO to make test pass.
	public void testGenerateFilesForSubsequentRelease(@Injectable final Execution execution, 
			@Injectable final ExecutionDAO dao) throws Exception
			{
		final List<Package> packages = createPackages( false );
		new NonStrictExpectations() {{
			execution.getBuild();
			returns( build);
			returns(false);
			build.getPackages();
			returns(packages);
		}};

		final List<String> fileNames = mockTransformedFileNames();
		new Expectations(){{
			dao.listTransformedFilePaths( withInstanceOf(Execution.class),anyString );
			returns(fileNames);
			//only for delta file at the moment.
			dao.copyTransformedFileToOutput(execution, packages.get(0).getBusinessKey(), fileNames.get(0));

		}};
		
		
		for( Package pkg : packages ){
			ReleaseFileGenerator generator = new SubsequentReleaseFileGenerator(execution, pkg, dao);
			generator.generateReleaseFiles();
		}

	}



	private List<String> mockTransformedFileNames() {
		final List<String> fileNames = new ArrayList<>();
		fileNames.add(DELTA_FILE_NAME);
		return fileNames;
	}
	
	private List<Package> createPackages( boolean isFirstRelease) {
		List<Package> packages = new ArrayList<>();
		Package pk = new Package("PK1");
		pk.setBuild(build);
		pk.setFirstTimeRelease(isFirstRelease);
		packages.add(pk);
		return packages;
	}

}
