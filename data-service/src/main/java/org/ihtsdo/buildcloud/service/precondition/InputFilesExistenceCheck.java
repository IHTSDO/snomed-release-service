package org.ihtsdo.buildcloud.service.precondition;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.*;

/**
 * To check all files specified in the manifest file can be derived from the input files.
 * If not the product process will be halted and the release manager should be alerted.
 * RF2 files:Full/snapshot/delta
 * der2_Refset_SimpleSnapshot_INT_20140831.txt
 * sct2_Concept_Snapshot_INT_20140131.txt
 * Documentation:
 * doc_Icd10MapTechnicalGuideExemplars_Current-en-US_INT_20140131.xlsx
 * doc_SnomedCTReleaseNotes_Current-en-US_INT_20140131.pdf
 * Readme files:
 * Readme_en_20140131.txt
 * Resources:
 * zres2_icRefset_OrderedTypeFull_INT_20110731.txt
 */
@Service
public class InputFilesExistenceCheck extends PreconditionCheck {

	private static final String MISSING_STATED_RELATIONSHIP_FILE = "No stated relationship file is found in the input file directory.";
	private static final String MISSING_RELATIONSHIP_FILE = "No relationship file is found in the input file directory.";
	private static final String STATED_RELATIONSHIP = "_StatedRelationship_";
	private static final String RELATIONSHIP = "_Relationship_";
	private static final String ERROR_MSG = "The input files directory doesn't contain the following files required by the manifest.xml: ";
	@Autowired
	private BuildDAO buildDAO;

	@Override
	public void runCheck(final Build build) {
		//check against the manifest file
		boolean isFailed = false;

		// If this is a beta build, manifest may specify x prefix.
		final boolean isBeta = build.getConfiguration().isBetaRelease();

		try (InputStream manifestInputSteam = buildDAO.getManifestStream(build)) {
			final ManifestXmlFileParser parser = new ManifestXmlFileParser();
			final ListingType listingType = parser.parse(manifestInputSteam);
			final boolean justPackage = build.getConfiguration().isJustPackage();
			final Set<String> filesExpected = resolveExpectedFilesByManifest(isBeta, justPackage, listingType);
			//get a list of input file names
			final List<String> inputfilesList = buildDAO.listInputFileNames(build);
			//check expected against input files
			final StringBuilder msgBuilder = new StringBuilder();
			int count = 0;
			for (final String expectedFileName : filesExpected) {
				// If this is a beta build, then the input files may optionally miss out the 'x' prefix
				String acceptableFileName = expectedFileName;
				if (!justPackage && isBeta && expectedFileName.startsWith(BuildConfiguration.BETA_PREFIX)) {
					acceptableFileName = expectedFileName.substring(1);
				}
				if (!inputfilesList.contains(expectedFileName) && !inputfilesList.contains(acceptableFileName)) {
					if (count++ > 0) {
						msgBuilder.append(",");
					}
					msgBuilder.append(isBeta ? acceptableFileName : expectedFileName);
				}
			}
			final StringBuilder errorMsgBuilder = new StringBuilder();
			if (count > 0) {
				errorMsgBuilder.append(ERROR_MSG);
				errorMsgBuilder.append(msgBuilder.toString());
				errorMsgBuilder.append(".");
				fatalError(errorMsgBuilder.toString());
				return;
			}
			//check stated relationship delta file present
			if (!justPackage) {
				boolean isStatedRelationshipFilePresent = false;
				boolean isRelationshipFilePresent = false;
				for ( final String name : inputfilesList) {
					if (name.contains(STATED_RELATIONSHIP)) {
						isStatedRelationshipFilePresent = true;
					}
					if (name.contains(RELATIONSHIP)) {
						isRelationshipFilePresent = true;
					}
				}
				if (!isStatedRelationshipFilePresent || !isRelationshipFilePresent) {
					if (!isStatedRelationshipFilePresent) {
						if (errorMsgBuilder.length() > 0 ) {
							errorMsgBuilder.append(" ");
						}
						errorMsgBuilder.append(MISSING_STATED_RELATIONSHIP_FILE);
					}

					if (!isRelationshipFilePresent) {
						if (errorMsgBuilder.length() > 0 ) {
							errorMsgBuilder.append(" ");
						}
						errorMsgBuilder.append(MISSING_RELATIONSHIP_FILE);
					}
				}
			}
			if (errorMsgBuilder.length() > 0) {
				// Missing files is no longer considered FATAL
				fail(errorMsgBuilder.toString());
				isFailed = true;
			}

		} catch (JAXBException | ResourceNotFoundException | IOException e) {
			fatalError("Failed to parse manifest xml file." + e.getMessage());
			isFailed = true;
		}
		if (!isFailed) {
			pass();
		}

	}

	private Set<String> resolveExpectedFilesByManifest(final boolean isBeta, final boolean isJustPackaging, final ListingType listingType) {
		final Set<String> filesExpected = new HashSet<>();
		boolean isFirstReadmeFound = false;
		//all files specified in the manifest file must be present in the input folder 
		//apart from the Readme txt file which is generated by SRS.
		for ( String fileName : ManifestFileListingHelper.listAllFiles(listingType)) {
			if (fileName == null || fileName.trim().length() == 0) {
				continue;
			}
			//dealing with der2 and sct2 full/snapshot/delta files
			final String[] splits = fileName.split(FILE_NAME_SEPARATOR);
			String fileNamePrefix = splits[0];
			if (!isFirstReadmeFound && README_FILENAME_PREFIX.equals(fileNamePrefix)) {
				//ignore the first one as it will be auto generated by release service.
				isFirstReadmeFound = true;
				continue;
			}
			if (RELEASE_INFORMATION_FILENAME_PREFIX.equals(fileNamePrefix)) {
				//ignore the release file
				continue;
			}
			if (!isJustPackaging) {
				if (isBeta && fileNamePrefix.startsWith(BuildConfiguration.BETA_PREFIX)) {
					fileNamePrefix = fileNamePrefix.substring(1);
				}
				if (DER2.equals(fileNamePrefix) || SCT2.equals(fileNamePrefix)) {
					if (fileName.contains(SCT2 + RELATIONSHIP)) {
						fileName = fileName.replace(SCT2 + RELATIONSHIP, SCT2 + STATED_RELATIONSHIP);
					}
					fileName = fileName.replace(fileNamePrefix, INPUT_FILE_PREFIX);
					final String token3 = splits[2];
					if (token3.contains(FULL)) {
						fileName = fileName.replace(FULL, DELTA);
					} else if (token3.contains(SNAPSHOT)) {
						fileName = fileName.replace(SNAPSHOT, DELTA);
					}
				} 
			}
		filesExpected.add(fileName);
		}
		return filesExpected;
	}
}
