package org.ihtsdo.buildcloud.core.service.validation.precondition;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.manifest.FolderType;
import org.ihtsdo.buildcloud.core.manifest.ListingType;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.helper.ManifestXmlFileParser;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ManifestCheck extends PreconditionCheck {

	private static final String COMMA = ",";
	private static final String HYPHEN = "_";
	private static final String RELEASE_DATE_NOT_MATCHED_MSG = "The following file names specified in the manifest:%s don't have "
			+ "the same release date as configured in the product:%s.";
	private static final String PRESENT_IN_SNAPSHOT_BUT_NOT_IN_FULL_MSG = "The following file names specified in the Snapshot folder but not found in the Full folder: %s";
	private static final String PRESENT_IN_FULL_BUT_NOT_IN_SNAPSHOT_MSG = "The following file names specified in the Full folder but not found in the Snapshot folder: %s";
	private static final String INVALID_RELEASE_PACKAGE_NAME_MSG = "The package name does not follow the packaging conventions: "
			+ "[x prefix if applicable]SnomedCT_[Product][Format(optional)]_[ReleaseStatus]_[Releasedate]T[Releasetime]Z";
	private static final String INVALID_FILE_NAME_AGAINST_BETA_RELEASE_MSG = "The following files which specified in the manifest are required starting with x for a Beta release: %s";
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

	@Value("${srs.release.package.pattern}")
	private String releasePackagePattern;

	@Override
	public void runCheck(final Build build) {
		try (InputStream manifestInputSteam = buildDAO.getManifestStream(build)) {
			//check that a manifest file is present.
			//check that the manifest conforms to the XSD and specifically, that we can find a valid root folder
			final ManifestXmlFileParser parser = new ManifestXmlFileParser();
			final ListingType manifestListing = parser.parse(manifestInputSteam);
			final String releaseVersion = build.getConfiguration().getEffectiveTimeSnomedFormat();
			if (releaseVersion != null) {
				final String invalidFileNamesInFolderMsg = validateFileNamesAgainstFolder(manifestListing);
				if (StringUtils.hasLength(invalidFileNamesInFolderMsg)) {
					fatalError(invalidFileNamesInFolderMsg);
					return;
				}

				if (build.getConfiguration().isBetaRelease()) {
					final String invalidFileNamesAgainstBetaReleaseMsg = validateFileNamesAgainstBetaRelease(manifestListing);
					if (StringUtils.hasLength(invalidFileNamesAgainstBetaReleaseMsg)) {
						fatalError(invalidFileNamesAgainstBetaReleaseMsg);
						return;
					}
				}

				final String invalidFileFormatMsg = validateFileFormat(manifestListing, releaseVersion);
				if (StringUtils.hasLength(invalidFileFormatMsg)) {
					fail(invalidFileFormatMsg);
					return;
				}

				final String invalidFilePresentMsg = validateManifestStructure(manifestListing);
				if (StringUtils.hasLength(invalidFilePresentMsg)) {
					fail(invalidFilePresentMsg);
					return;
				}

				final String invalidReleaseVersionMsg = validateReleaseDate(manifestListing, releaseVersion);
				final boolean validReleasePackageName = validatePackageName(manifestListing);
				if (StringUtils.hasLength(invalidReleaseVersionMsg) || !validReleasePackageName) {
					String warningMsg = "";
					if (StringUtils.hasLength(invalidReleaseVersionMsg)) {
						warningMsg = invalidReleaseVersionMsg;
					}
					if (!validReleasePackageName) {
						warningMsg = StringUtils.hasLength(invalidReleaseVersionMsg) ?
								warningMsg + " " + INVALID_RELEASE_PACKAGE_NAME_MSG : INVALID_RELEASE_PACKAGE_NAME_MSG;
					}
					warning(warningMsg);
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

	private String validateManifestStructure(final ListingType manifestListing) {
		List<FolderType> folderTypes = manifestListing.getFolder().getFolder();
		List<String> snapshotFiles = new ArrayList<>();
		List<String> fullFiles = new ArrayList<>();
		for (FolderType folderType : folderTypes) {
			String folderName = folderType.getName();
			if (RF2Constants.SNAPSHOT.equals(folderName)) {
				snapshotFiles = ManifestFileListingHelper.getFilesByFolderName(manifestListing, folderName);
			} else if (RF2Constants.FULL.equals(folderName)) {
				fullFiles = ManifestFileListingHelper.getFilesByFolderName(manifestListing, folderName);
			}
		}
		if (!snapshotFiles.isEmpty() && !fullFiles.isEmpty()) {
			List <String> finalFullFiles = fullFiles;
			List <String> finalSnapshotFiles = snapshotFiles;
			List<String> missingFilesInFullFolder = snapshotFiles.stream()
					.filter(file -> !finalFullFiles.contains(file.replace("Snapshot_", "Full_").replace("Snapshot-", "Full-")))
					.collect(Collectors.toList());
			List<String> missingFilesInSnapshotFolder = fullFiles.stream()
					.filter(file -> !finalSnapshotFiles.contains(file.replace("Full_", "Snapshot_").replace("Full-", "Snapshot-")))
					.collect(Collectors.toList());
			if (!missingFilesInFullFolder.isEmpty()) {
				return String.format(PRESENT_IN_SNAPSHOT_BUT_NOT_IN_FULL_MSG, String.join(", ", missingFilesInFullFolder));
			}
			if (!missingFilesInSnapshotFolder.isEmpty()) {
				return String.format(PRESENT_IN_FULL_BUT_NOT_IN_SNAPSHOT_MSG, String.join(", ", missingFilesInSnapshotFolder));
			}
		}
		return null;
	}

	private String validateFileNamesAgainstBetaRelease(ListingType manifestListing) {
		final StringBuilder invalidFileNameMsgBuilder = new StringBuilder();
		final String zipFileName = manifestListing.getFolder().getName();

		// check package name
		if (StringUtils.hasLength(zipFileName) && !zipFileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
			invalidFileNameMsgBuilder.append(zipFileName);
		}
		//check that sct2 and der2 file names starting with X
		final List<String> fileNames = ManifestFileListingHelper.listAllFiles(manifestListing);
		for (final String fileName : fileNames) {
			if (!fileName.startsWith(RF2Constants.README_FILENAME_PREFIX)
				&& !fileName.startsWith(RF2Constants.RELEASE_INFORMATION_FILENAME_PREFIX)
				&& !fileName.startsWith(RF2Constants.BETA_RELEASE_PREFIX)) {
				if (invalidFileNameMsgBuilder.length() > 0) {
					invalidFileNameMsgBuilder.append(COMMA);
				}
				invalidFileNameMsgBuilder.append(fileName);
			}
		}
		if (invalidFileNameMsgBuilder.length() > 0) {
			return String.format(INVALID_FILE_NAME_AGAINST_BETA_RELEASE_MSG, invalidFileNameMsgBuilder.toString());
		}
		return "";
	}

	private String validateFileFormat(final ListingType manifestListing, final String releaseVersion) {
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
			if (fileName.startsWith(RF2Constants.SCT2) || fileName.startsWith(RF2Constants.DER2) || fileName.startsWith(SCT1) || fileName.startsWith(DER1)) {
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

	private String validateReleaseDate(final ListingType manifestListing, final String releaseVersion) {
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
			if (fileName.startsWith(RF2Constants.SCT2) || fileName.startsWith(RF2Constants.DER2) || fileName.startsWith(SCT1) || fileName.startsWith(DER1)) {
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

	private boolean validatePackageName(ListingType manifestListing) {
		String packageName = manifestListing.getFolder().getName();
		Pattern pattern = Pattern.compile(this.releasePackagePattern);
		Matcher matcher = pattern.matcher(packageName);
		return matcher.matches();
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
