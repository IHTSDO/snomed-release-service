package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.DER2;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SCT2;

public class ManifestCheck extends PreconditionCheck {

	private static final String RELEASE_DATE_NOT_MATCHED_MSG = "The following file names specified in the manifest:%s don't have " +
			"the same release date as configured in the product:%s";

	@Autowired
	private BuildDAO buildDAO;

	@Override
	public void runCheck(final Build build) {
		try (InputStream manifestInputSteam = buildDAO.getManifestStream(build)) {
			//check that a manifest file is present.
			//check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
			ManifestXmlFileParser parser = new ManifestXmlFileParser();
			ListingType manifestListing = parser.parse(manifestInputSteam);
			//check release date in manifest
			String releaseVersion = build.getProduct().getEffectiveTimeSnomedFormat();
			if (releaseVersion != null) {
				String errorMsg = checkReleaseDate(manifestListing, releaseVersion);
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

	private String checkReleaseDate(final ListingType manifestListing, final String releaseVersion) {
		StringBuilder result = new StringBuilder();
		//check the root folder/zip file name has the correct date
		String zipFileName = manifestListing.getFolder().getName();
		if (!zipFileName.contains(releaseVersion)) {
			result.append(zipFileName);
		}
		//check that sct2 and der2 file names have got the same date as the release date/version
		List<String> fileNames = ManifestFileListingHelper.listAllFiles(manifestListing);
		for (String fileName : fileNames) {
			if (fileName.startsWith(SCT2) || fileName.startsWith(DER2)) {
				if (!fileName.contains(releaseVersion)) {
					if (result.length() > 0) {
						result.append(",");
					}
					result.append(fileName);
				}
			}
		}
		if (result.length() > 0) {
			return String.format(RELEASE_DATE_NOT_MATCHED_MSG, result.toString(), releaseVersion);
		}
		return null;
	}
}
