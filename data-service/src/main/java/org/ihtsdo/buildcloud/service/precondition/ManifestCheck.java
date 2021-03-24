package org.ihtsdo.buildcloud.service.precondition;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.DER2;
import static org.ihtsdo.buildcloud.service.build.RF2Constants.SCT2;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

public class ManifestCheck extends PreconditionCheck {

	private static final String COMMA = ",";
	private static final String HYPHEN = "_";
	private static final String RELEASE_DATE_NOT_MATCHED_MSG = "The following file names specified in the manifest:%s don't have "
			+ "the same release date as configured in the product:%s.";
	private static final String INVALID_RELEASE_FILE_FORMAT_MSG = "The following file names specified in the manifest:%s don't follow naming convention:%s.";
	private static final String FILE_NAME_CONVENTION = "<FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>";

	private static final String DER1 = "der1";
	private static final String SCT1 = "sct1";
	private static final String EMPTY_FILE_NAME_MSG = "Total number of files with no file name specified: %d";
	private static final String README_FILE_NOT_FOUND_MSG = "No Readme file ends with .txt found in manifest.";
	private static final String INVALID_FILES_IN_FOLDER = "Invalid files in %s folder: %s.";
	private static final Logger LOGGER = LoggerFactory.getLogger(ManifestCheck.class);

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
				String invalidFileNamesInFolderMsg = validateFileNamesAgainstFolder(manifestListing);
				if (!StringUtils.isEmpty(invalidFileNamesInFolderMsg)) {
					fatalError(invalidFileNamesInFolderMsg);
					return;
				}

				final String errorMsg = validate(manifestListing, releaseVersion);
				if (!StringUtils.isEmpty(errorMsg)) {
					fail(errorMsg);
					return;
				}

				String invalidReleaseVersionMsg = validateReleaseVersion(manifestListing, releaseVersion);
				if (!StringUtils.isEmpty(invalidReleaseVersionMsg)) {
					warning(invalidReleaseVersionMsg);
					return;
				}
			}
			pass();
		} catch (ResourceNotFoundException | JAXBException | IOException e) {
			LOGGER.error("Exception thrown when validating manifest file for build:{}", build.getId());
			e.printStackTrace();
			fatalError("Build manifest is missing or invalid: " + e.getMessage());
		}
	}

	private String validate(final ListingType manifestListing, final String releaseVersion) {
		final StringBuilder invalidFileNameMsgBuilder = new StringBuilder();
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
				continue;
			}
		}


		final StringBuilder result = new StringBuilder();
		if (invalidFileNameMsgBuilder.length() > 0) {
			result.append(String.format(INVALID_RELEASE_FILE_FORMAT_MSG,invalidFileNameMsgBuilder.toString(), FILE_NAME_CONVENTION));
		}
		if (!isReadmeFound) {
			result.append(README_FILE_NOT_FOUND_MSG);
		}
		if (emptyFileNameCount > 0) {
			result.append(String.format(EMPTY_FILE_NAME_MSG, emptyFileNameCount));
		}
		if (result.length() > 0) {
			return result.toString();
		}
		return null;
	}

	private String validateReleaseVersion(final ListingType manifestListing, final String releaseVersion) {
		final StringBuilder releaseVersionMsgBuilder = new StringBuilder();

		final String zipFileName = manifestListing.getFolder().getName();
		if (!zipFileName.contains(releaseVersion)) {
			releaseVersionMsgBuilder.append(zipFileName);
		}
		//check that sct2 and der2 file names have got the same date as the release date/version
		final List<String> fileNames = ManifestFileListingHelper.listAllFiles(manifestListing);
		for (final String fileName : fileNames) {
			//check file name is not empty
			if (fileName == null || fileName.trim().length() == 0) {
				continue;
			}
			if (fileName.startsWith(RF2Constants.README_FILENAME_PREFIX) && fileName.endsWith(RF2Constants.README_FILENAME_EXTENSION)) {
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
		if (releaseVersionMsgBuilder.length() > 0) {
			result.append(String.format(RELEASE_DATE_NOT_MATCHED_MSG, releaseVersionMsgBuilder.toString(), releaseVersion));
		}
		if (result.length() > 0) {
			return result.toString();
		}
		return null;
	}
	private String validateFileNamesAgainstFolder(final ListingType manifestListing) {
		List<FolderType> folderTypes = manifestListing.getFolder().getFolder();
		final StringBuilder result = new StringBuilder();
		for (FolderType folderType : folderTypes) {
			String folderName = folderType.getName();
			List<String> fileNames = ManifestFileListingHelper.getFilesByFolderName(manifestListing, folderName);
			List<String> invalidFileNames;
			if (RF2Constants.DELTA.equals(folderName)) {
				invalidFileNames = getFileNamesContainsAny(fileNames, RF2Constants.SNAPSHOT + HYPHEN, RF2Constants.FULL + HYPHEN);
				if (!invalidFileNames.isEmpty()) {
					result.append(String.format(INVALID_FILES_IN_FOLDER, RF2Constants.DELTA, String.join(COMMA, invalidFileNames)));
				}
			} else if (RF2Constants.SNAPSHOT.equals(folderName)) {
				invalidFileNames = getFileNamesContainsAny(fileNames, RF2Constants.DELTA + HYPHEN, RF2Constants.FULL + HYPHEN);
				if (!invalidFileNames.isEmpty()) {
					result.append(String.format(INVALID_FILES_IN_FOLDER, RF2Constants.SNAPSHOT, String.join(COMMA, invalidFileNames)));
				}
			} else if (RF2Constants.FULL.equals(folderName)) {
				invalidFileNames =  getFileNamesContainsAny(fileNames, RF2Constants.DELTA + HYPHEN, RF2Constants.SNAPSHOT + HYPHEN);
				if (!invalidFileNames.isEmpty()) {
					result.append(String.format(INVALID_FILES_IN_FOLDER, RF2Constants.FULL, String.join(COMMA, invalidFileNames)));
				}
			} else  {
				// do nothing
			}
		}

		return  result.toString();
	}

	private List<String> getFileNamesContainsAny(List<String> fileNames, String... patterns) {
		List<String> result = new ArrayList<>();
		for (String fileName : fileNames) {
			if (Arrays.stream(patterns).parallel().anyMatch(fileName::contains)) {
				result.add(fileName);
			}
		}

		return result;
	}
}
