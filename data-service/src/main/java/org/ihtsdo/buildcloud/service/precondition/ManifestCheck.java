package org.ihtsdo.buildcloud.service.precondition;

import static org.ihtsdo.buildcloud.service.execution.RF2Constants.DER2;
import static org.ihtsdo.buildcloud.service.execution.RF2Constants.SCT2;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.springframework.beans.factory.annotation.Autowired;

public class ManifestCheck extends PreconditionCheck {

	private static final String RELEASE_DATE_NOT_MATCHED_MSG = "The following file names specified in the manifest:%s don't have the same release date as configured in the build:%s";
	@Autowired
	private ExecutionDAO executionDAO;

	@Override
	public void runCheck(final Package pkg, final Execution execution) {

	    try (InputStream manifestInputSteam = executionDAO.getManifestStream(execution, pkg))
	    {
		//check that a manifest file is present.  
		//check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
		ManifestXmlFileParser parser = new ManifestXmlFileParser();
		ListingType manifestListing = parser.parse(manifestInputSteam);
		//check release date in manifest
		String releaseVersion = execution.getBuild().getEffectiveTimeSnomedFormat();
		if (releaseVersion != null){
		  String errorMsg = checkReleaseDate(manifestListing, releaseVersion);
		  if (errorMsg != null){
		      fail(errorMsg);
		      return;
		  }
		}
		pass();
	    } catch (Exception e) {
		fatalError("Package manifest is missing or invalid: " + e.getMessage());
	    }
	}

	private String checkReleaseDate(final ListingType manifestListing, final String releaseVersion) {
	    StringBuilder result = new StringBuilder();
	    //check the root folder/zip file name has the correct date
	        String zipFileName = manifestListing.getFolder().getName();
		if (!zipFileName.contains(releaseVersion)){
	    	  result.append(zipFileName);
	        }
	    	//check that sct2 and der2 file names have got the same date as the release date/version
	    	List<String> fileNames = ManifestFileListingHelper.listAllFiles(manifestListing);
	    	for (String fileName : fileNames){
	    	    if (fileName.startsWith(SCT2) || fileName.startsWith(DER2)){
	    		if (!fileName.contains(releaseVersion)){
	    		    if (result.length() > 0){
	    			result.append(",");
	    		    }
	    		    result.append(fileName);
	    		}
	    	    }
	    	}
	    if ( result.length() > 0 ){
		return String.format(RELEASE_DATE_NOT_MATCHED_MSG, result.toString(), releaseVersion);
	    } 
	    return null;
	}
}
