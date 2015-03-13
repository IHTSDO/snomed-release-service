package org.ihtsdo.buildcloud.service.precondition;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.DER2;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SCT2;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ManifestCheck extends PreconditionCheck {

	private static final String COMMA = ",";
	private static final String RELEASE_DATE_NOT_MATCHED_MSG = "The following file names specified in the manifest:%s don't have " 
			+ "the same release date as configured in the product:%s.";
	private static final String INVALID_RELEASE_FILE_FORMAT_MSG = "The following file names specified in the manifest:%s don't follow naming convention:%s.";
	private static final String FILE_NAME_CONVENTION = "<FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>";
	
	private static final String DER1 = "der1";
	private static final String SCT1 = "sct1";
	private static final String EMPTY_FILE_NAME_MSG = "Total number of files with no file name specified: %d";

	@Autowired
	private BuildDAO buildDAO;

	@Override
	public void runCheck(final Build build) {
		try (InputStream manifestInputSteam = buildDAO.getManifestStream(build)) {
			//check that a manifest file is present.
			//check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
			final ManifestXmlFileParser parser = new ManifestXmlFileParser();
			final ListingType manifestListing = parser.parse(manifestInputSteam);
			final String releaseVersion = build.getConfiguration().getEffectiveTimeSnomedFormat();
			if (releaseVersion != null) {
				final String errorMsg = validate(manifestListing, releaseVersion);
				if (errorMsg != null) {
					fail(errorMsg);
					return;
				}
			}
			pass();
		} catch (ResourceNotFoundException | JAXBException | IOException e) {
			fatalError("Build manifest is missing or invalid: " + e.getMessage());
		}
	}

	private String validate(final ListingType manifestListing, final String releaseVersion) {
		final StringBuilder releaseVersionMsgBuilder = new StringBuilder();
		final StringBuilder invalidFileNameMsgBuilder = new StringBuilder();
		//check the root folder/zip file name has the correct date
		final String zipFileName = manifestListing.getFolder().getName();
		if (!zipFileName.contains(releaseVersion)) {
			releaseVersionMsgBuilder.append(zipFileName);
		}
		//check that sct2 and der2 file names have got the same date as the release date/version
		final List<String> fileNames = ManifestFileListingHelper.listAllFiles(manifestListing);
		boolean isReadmeFound = false;
		int emptyFileNameCount = 0;
		for (final String fileName : fileNames) {
			//check file name is not empty
			if (fileName == null || fileName.trim().length() == 0) {
				emptyFileNameCount++;
				continue;
			}
			//check readme txt file
			if (!isReadmeFound && fileName.startsWith(RF2Constants.README_FILENAME_PREFIX) && fileName.endsWith(RF2Constants.README_FILENAME_EXTENSION)) {
				//Readme_20140831.txt
				isReadmeFound = true;
				if (fileName.split(RF2Constants.FILE_NAME_SEPARATOR).length >= 2) {
					if (!fileName.contains(releaseVersion)) {
						if (releaseVersionMsgBuilder.length() > 0) {
							releaseVersionMsgBuilder.append(COMMA);
						}
						releaseVersionMsgBuilder.append(fileName);
					}
				}
				continue;
			}
			// check RF1 and RF2 file name convention 
			if (fileName.startsWith(SCT2) || fileName.startsWith(DER2) || fileName.startsWith(SCT1) || fileName.startsWith(DER1)) {
				final String[] tokens = fileName.split(RF2Constants.FILE_NAME_SEPARATOR);
				if (tokens.length != 5) {
					if (invalidFileNameMsgBuilder.length() > 0) {
						invalidFileNameMsgBuilder.append(COMMA);
					}
					invalidFileNameMsgBuilder.append(fileName);
				}
				if (!tokens[tokens.length - 1].contains(releaseVersion)) {
					if (releaseVersionMsgBuilder.length() > 0) {
						releaseVersionMsgBuilder.append(COMMA);
					}
					releaseVersionMsgBuilder.append(fileName);
				}
				continue;
			}
		}
		final StringBuilder result = new StringBuilder();
		if (invalidFileNameMsgBuilder.length() > 0) {
			result.append(String.format(INVALID_RELEASE_FILE_FORMAT_MSG,invalidFileNameMsgBuilder.toString(), FILE_NAME_CONVENTION));
		}
		if (releaseVersionMsgBuilder.length() > 0) {
			result.append(String.format(RELEASE_DATE_NOT_MATCHED_MSG, releaseVersionMsgBuilder.toString(), releaseVersion));
		}
		if (!isReadmeFound) {
			result.append("No Readme file ends with .txt found in manifest.");
		}
		if (emptyFileNameCount > 0) {
			result.append(String.format(EMPTY_FILE_NAME_MSG, emptyFileNameCount));
		}
		if (result.length() > 0) {
			return result.toString();
		}
		return null;
	}
}
