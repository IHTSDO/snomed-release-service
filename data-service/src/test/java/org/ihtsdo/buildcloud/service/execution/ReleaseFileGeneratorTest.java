package org.ihtsdo.buildcloud.service.execution;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import mockit.integration.junit4.JMockit;
import mockit.*;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.FileService;
import org.ihtsdo.buildcloud.entity.Package;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JMockit.class)
public class ReleaseFileGeneratorTest {

	protected static final String DELTA_FILE_NAME = "der2_Refset_SimpleDelta_INT_20140831.txt";
	@Mocked Build build;
	@Test
	public void testGenerateReleaseFiles(@Injectable final Execution execution, @Injectable final FileService fileService) throws IOException
	{
		final List<Package> packages = createPackages();
		final List<String> fileNames = mockTransformedFileNames();
		new NonStrictExpectations() {{
			execution.getBuild();
			returns( build);
			build.isFirstTimeRelease();
			returns(true);
			execution.getBuild();
			returns( build);
			build.getPackages();
			returns(packages);
			fileService.listTransformedFilePaths( withInstanceOf(Execution.class),anyString );
			returns(fileNames);
		}};
		
		new NonStrictExpectations(2) { {
			
			for( Package pk : packages )
			{
				for( String deltaName : fileNames ){
					
					fileService.copyTransformedFileToOutput(execution, pk.getBusinessKey(), deltaName, anyString);
				}
			}
			
		} };
		
		String deltaFile = getClass().getResource("/org/ihtsdo/buildcloud/service/execution/"+ DELTA_FILE_NAME).getFile();
		final InputStream inputStream = new FileInputStream(deltaFile);
	
		String outputDeltaFile = deltaFile.replace(".txt", "_output.txt");
		final OutputStream outputStream = new FileOutputStream( outputDeltaFile);
		new Expectations() { {
			
			for( Package pk : packages )
			{
				for( String fileName : fileNames ){
					
					fileService.getTransformedFileAsInputStream(execution, pk.getBusinessKey(), fileName);
					returns(inputStream);
					fileService.getExecutionOutputFileOutputStream(execution, pk.getBusinessKey(), fileName);
					returns(outputStream);
					
				}
			}
			
		}};
		ReleaseFileGenerator generator = new ReleaseFileGenerator(execution, fileService);
		generator.generateReleaseFiles();
		
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
	public void testGenerateFilesForSubsequentRelease(@Injectable final Execution execution, 
			@Injectable final FileService fileService) throws IOException
			{
		final List<Package> packages = createPackages();
		new NonStrictExpectations() {{
			execution.getBuild();
			returns( build);
			build.isFirstTimeRelease();
			returns(false);
			build.getPackages();
			returns(packages);
		}};

		final List<String> fileNames = mockTransformedFileNames();
		new Expectations(){{
			fileService.listTransformedFilePaths( withInstanceOf(Execution.class),anyString );
			returns(fileNames);
			//only for delta file at the moment.
			fileService.copyTransformedFileToOutput(execution, packages.get(0).getBusinessKey(), fileNames.get(0));

		}};
		ReleaseFileGenerator generator = new ReleaseFileGenerator(execution, fileService);
		generator.generateReleaseFiles();

			}



	private List<String> mockTransformedFileNames() {
		final List<String> fileNames = new ArrayList<>();
		fileNames.add(DELTA_FILE_NAME);
		return fileNames;
	}
	
	private List<Package> createPackages() {
		List<Package> packages = new ArrayList<>();
		Package pk = new Package("PK1");
		packages.add(pk);
		return packages;
	}

}
