package org.ihtsdo.buildcloud.service.precondition;

import static org.ihtsdo.buildcloud.service.execution.RF2Constants.*;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * To check all files specified in the manifest file can be derived from the input files.
 * If not the build process will be halted and the release manager should be alerted.
 * RF2 files:Full/snapshot/delta
 * der2_Refset_SimpleSnapshot_INT_20140831.txt
 * sct2_Concept_Snapshot_INT_20140131.txt
 * Documentation:
 * doc_Icd10MapTechnicalGuideExemplars_Current-en-US_INT_20140131.xlsx
 * doc_SnomedCTReleaseNotes_Current-en-US_INT_20140131.pdf
 * Readme files:
 * Readme_en_20140131.txt
 *  Resources:
 * zres2_icRefset_OrderedTypeFull_INT_20110731.txt
 *
 */
public class InputFilesExistenceCheck extends PreconditionCheck {

    private static final String ERROR_MSG = "The file required by manifest but doesn't exist in the input files directory:";
    @Autowired
    private ExecutionDAO executionDAO;
    
    @Override
    public void runCheck(Package pkg, Execution execution) {
	//check against the manifest file
	InputStream manifestInputSteam = executionDAO.getManifestStream(execution, pkg);
	 boolean isFailed = false;
	ListingType listingType;
	try {
	    listingType = parseManifestFile(manifestInputSteam);
	    List<String> filesFromManifiest = ManifestFileListingHelper.listAllFiles(listingType);
	    Set<String> filesExpected = new HashSet<>();
	    boolean isFirstReadmeFound=false;
	    for(String fileName : filesFromManifiest){
		//it shouldn't be null double check anyway.
		if( fileName == null)
		{
		    continue;
		}
		//dealing with der2 and sct2 full/snapshot/delta files
		String[] splits=fileName.split(FILE_NAME_SEPARATOR);
		String fileNamePrefix = splits[0];
		if(DER2.equals(fileNamePrefix) || SCT2.equals(fileNamePrefix)){
		    String token3 = splits[2];
		    String temp = fileName;
		    if( token3.contains(FULL)){
			temp=fileName.replace(FULL, DELTA);
		    }
		    else if( token3.contains(SNAPSHOT)){
			temp=fileName.replace(SNAPSHOT, DELTA);
		    }
		    filesExpected.add(temp.replace(fileNamePrefix, INPUT_FILE_PREFIX));
		}
		else if(!isFirstReadmeFound && README_FILENAME_PREFIX.equals(fileNamePrefix)){
		    //ignore the first one as it will be auto generated by release service.
		    isFirstReadmeFound=true;
		}
		else{
		    //add all static documentation and resources files.
		    filesExpected.add(fileName);
		}
		
	    }
	  //get a list of input file names
	    List<String> inpufilesList=executionDAO.listInputFileNames(execution, pkg.getBusinessKey());
	    //check expected against input files 
	    for(String expectedFileName : filesExpected){
		if(!inpufilesList.contains(expectedFileName)){
		    fatalError( ERROR_MSG + expectedFileName);
		    isFailed = true;
		}
	    }
	   
	} catch (JAXBException | ResourceNotFoundException e) {
	    fatalError("Failed to load manifest file." + e.getMessage());
	    isFailed=true;
	}
	if( !isFailed){
	    pass();
	}
	
    }
    
    private ListingType parseManifestFile(InputStream manifestInputSteam) throws JAXBException, ResourceNotFoundException{
	//Get the manifest file as an input stream
	if ( manifestInputSteam == null) {
		throw new ResourceNotFoundException ("Failed to load manifest due to null inputstream");
	}
	//Load the manifest file xml into a java object hierarchy
	JAXBContext jc = JAXBContext.newInstance( MANIFEST_CONTEXT_PATH);
	Unmarshaller um = jc.createUnmarshaller();
	return um.unmarshal(new StreamSource(manifestInputSteam), ListingType.class).getValue();
    }

}
