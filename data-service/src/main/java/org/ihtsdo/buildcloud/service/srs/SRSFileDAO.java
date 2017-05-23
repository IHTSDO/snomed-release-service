package org.ihtsdo.buildcloud.service.srs;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.dao.s3.S3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.FileHelper;
import org.ihtsdo.otf.rest.exception.ProcessWorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SRSFileDAO {

	private static final String DELTA = "Delta";
	private static final String SNAPSHOT = "Snapshot";

	private static final String INTERNATIONAL = "international";

	private static final String TXT = ".txt";

	private final Logger logger = LoggerFactory.getLogger(SRSFileDAO.class);

	private static final String FILE_TYPE_INSERT = "****";
	private static final String RELEASE_DATE_INSERT = "########";
	private static final String COUNTRY_OR_NAMSPACE ="$$$";

	public static final String UNKNOWN_EFFECTIVE_DATE = "Unpublished";
	public static final int EFFECTIVE_DATE_COLUMN = 1;
	public static final int CHARACTERISTIC_TYPE_ID_COLUMN = 8;
	public static final int REFSET_ID_COLUMN = 4;
	public static final int TYPE_ID_COLUMN = 6;

	public static final String STATED_RELATIONSHIP_SCTID = "900000000000010007";
	public static final String TEXT_DEFINITION_SCTID = "900000000000550004";
	public static final String ICDO_REFSET_ID = "446608001";
	public static Set<String> ACCEPTABLE_SIMPLEMAP_VALUES;
	public static final String LINE_ENDING = "\r\n";

	private static final String[] FILE_NAMES_TO_BE_EXCLUDED = {"der2_iissscRefset_ICD-9-CMEquivalenceComplexMapReferenceSet"};

	private static final String[] EXTENSION_EXCLUDED_FILES = {"der2_iisssccRefset_ICD-10ComplexMapReferenceSet","der2_sRefset_CTV3SimpleMap",
		"der2_sRefset_SNOMEDRTIDSimpleMap","der2_sRefset_ICD-OSimpleMapReferenceSet"};

	@Autowired
    S3ClientImpl s3Client;

	private String refsetBucket;
	private final boolean snowOwlFlatIndexExportStyle;

	static Map<String, RefsetCombiner> refsetMap;
	static {
		refsetMap = new HashMap<String, RefsetCombiner>();
		refsetMap.put("Simple", new RefsetCombiner("der2_Refset_Simple****_$$$_########.txt", new String[] {
				"der2_Refset_NonHumanSimpleReferenceSet****_$$$_########.txt",
				"der2_Refset_VirtualMedicinalProductSimpleReferenceSet****_$$$_########.txt",
				"der2_Refset_VirtualTherapeuticMoietySimpleReferenceSet****_$$$_########.txt", }));

		refsetMap.put("AssociationReference", new RefsetCombiner("der2_cRefset_AssociationReference****_$$$_########.txt", new String[] {
				"der2_cRefset_ALTERNATIVEAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_" +
						"MOVEDFROMAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_MOVEDTOAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_POSSIBLYEQUIVALENTTOAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_REFERSTOConceptAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_REPLACEDBYAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_SAMEASAssociationReferenceSet****_$$$_########.txt",
				"der2_cRefset_WASAAssociationReferenceSet****_$$$_########.txt", }));

		refsetMap.put("AttributeValue", new RefsetCombiner("der2_cRefset_AttributeValue****_$$$_########.txt", new String[] {
				"der2_cRefset_ConceptInactivationIndicatorReferenceSet****_$$$_########.txt",
				"der2_cRefset_DescriptionInactivationIndicatorReferenceSet****_$$$_########.txt", }));

		refsetMap.put("Language", new RefsetCombiner("der2_cRefset_Language****-en_$$$_########.txt", new String[] {
				"der2_cRefset_GBEnglish****-en-gb_$$$_########.txt", "der2_cRefset_USEnglish****-en-us_$$$_########.txt" }));

		refsetMap.put("RefsetDescriptor", new RefsetCombiner("der2_cciRefset_RefsetDescriptor****_$$$_########.txt", new String[] {}));

		refsetMap.put("DescriptionType", new RefsetCombiner("der2_ciRefset_DescriptionType****_$$$_########.txt",
				new String[] { "der2_ciRefset_DescriptionFormat****_$$$_########.txt" }));

		refsetMap.put("ExtendedMap", new RefsetCombiner("der2_iisssccRefset_ExtendedMap****_$$$_########.txt",
				new String[] { "der2_iisssccRefset_ICD-10ComplexMapReferenceSet****_$$$_########.txt" }));

		refsetMap.put("SimpleMap", new RefsetCombiner("der2_sRefset_SimpleMap****_$$$_########.txt", new String[] {
				"der2_sRefset_CTV3SimpleMap****_$$$_########.txt", "der2_sRefset_ICD-OSimpleMapReferenceSet****_$$$_########.txt",
				"der2_sRefset_SNOMEDRTIDSimpleMap****_$$$_########.txt", "der2_sRefset_GMDNSimpleMapReferenceSet****_$$$_########.txt" }));

		refsetMap.put("ModuleDependency", new RefsetCombiner("der2_ssRefset_ModuleDependency****_$$$_########.txt",
				new String[] { "der2_ssRefset_ModuleDependency****_$$$_########.txt" }));

		ACCEPTABLE_SIMPLEMAP_VALUES = new HashSet<String>();
		ACCEPTABLE_SIMPLEMAP_VALUES.add(ICDO_REFSET_ID);
	}

	public SRSFileDAO(String refsetBucket, boolean snowOwlFlatIndexExportStyle) {
		this.refsetBucket = refsetBucket;
		this.snowOwlFlatIndexExportStyle = snowOwlFlatIndexExportStyle;
	}
	
	
	public File extractAndConvertExportWithRF2FileNameFormat(File archive, String releaseCenter, String releaseDate, boolean includeExternalFiles) throws ProcessWorkflowException, IOException {
		// We're going to create release files in a temp directory
		File extractDir = Files.createTempDir();
		unzipFlat(archive, extractDir);
		logger.debug("Unzipped files to {}", extractDir.getAbsolutePath());
		
		String countryNamespace = getCountryOrNamespace(extractDir);
		logger.debug("Country or namespace found from file name:" + countryNamespace);
		
		renameDKTranslatedConceptsRefsetFile(extractDir, releaseDate);

		// Ensure all files have the correct release date
		enforceReleaseDate(extractDir, releaseDate);
		// suppress files that no longer to be released.
		suppressFilesNotRequired(FILE_NAMES_TO_BE_EXCLUDED, extractDir);
		//exclude files for extension releasee
		if (!INTERNATIONAL.equalsIgnoreCase(releaseCenter)){
			suppressFilesNotRequired(EXTENSION_EXCLUDED_FILES, extractDir);
		}

		if (snowOwlFlatIndexExportStyle) {
			renameFiles(extractDir, SNAPSHOT, DELTA);
		}
		// Merge the refsets into the expected files and replace any "unpublished" dates
		// with today's date
		mergeRefsets(extractDir, DELTA,countryNamespace, releaseDate);
		replaceInFiles(extractDir, UNKNOWN_EFFECTIVE_DATE, releaseDate, EFFECTIVE_DATE_COLUMN);
	

		// The description file is currently named sct2_Description_${extractType}-en-gb_INT_<date>.txt
		// and we need it to be sct2_Description_${extractType}-en_INT_<date>.txt
		File descriptionFileWrongName = new File(extractDir, "sct2_Description_Delta-en-gb_INT_" + releaseDate + TXT);
		File descriptionFileRightName = new File(extractDir, "sct2_Description_Delta-en_INT_" + releaseDate + TXT);
		if (descriptionFileWrongName.exists()) {
			descriptionFileWrongName.renameTo(descriptionFileRightName);
		} else {
			logger.warn("Was not able to find {} to correct the name", descriptionFileWrongName);
		}

		// We don't have a Text Definition file, so create that by extracting rows with TypeId 900000000000550004
		// from sct2_Description_Delta-en_INT_<date>.txt to form sct2_TextDefinition_Delta-en_INT_<date>.txt
		File description = new File(extractDir, "sct2_Description_Delta-en_INT_" + releaseDate + TXT);
		File definition = new File(extractDir, "sct2_TextDefinition_Delta-en_INT_" + releaseDate + TXT);
		createSubsetFile(description, definition, TYPE_ID_COLUMN, TEXT_DEFINITION_SCTID, true, false);

		//Now pull in an externally maintained refsets from S3
		if (includeExternalFiles) {
			includeExternallyMaintainedFiles(extractDir, releaseCenter, releaseDate);
		}
		return extractDir;
	}
	
	
	
	private String getCountryOrNamespace(File extractDir) {
		
		String[] rf2Filenames = extractDir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.startsWith("sct2_Concept_") && name.endsWith(TXT)) {
					return true;
				}
				return false;
			}
		});
		
		if (rf2Filenames.length == 1) {
			String[] splits = rf2Filenames[0].split("_");
			if (splits.length == 5) {
				return splits[3];
			}
		}
		return null;
	}


	/**
	 * rename dk and se translated concepts file.
	 *der2_Refset_554831000005107Delta_DK1000005_20160926.txt 
	 */
	private void renameDKTranslatedConceptsRefsetFile(File extractDir, String releaseDate) {
		File wrongName = new File(extractDir, "der2_Refset_554831000005107Delta_DK1000005_" + releaseDate + TXT);
		File updatedName = new File(extractDir, "der2_Refset_DanishTranslatedConceptsSimpleDelta_DK1000005_" + releaseDate + TXT);
		if (wrongName.exists()) {
			wrongName.renameTo(updatedName);
			logger.warn("found wrong file name:" + wrongName +  " and updated it to :" + updatedName);
		} 
	
	}

	private void suppressFilesNotRequired(String[] filenamesToBeExcluded, File extractDir) {
		
		List<String> filesToBeRemoved = new ArrayList<>();
		for (final String fileName : filenamesToBeExcluded) {
			String[] filesFound = extractDir.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if (name.startsWith(fileName)) {
						return true;
					}
					return false;
				}
			});
			filesToBeRemoved.addAll(Arrays.asList(filesFound));
		}
		for (String fileName : filesToBeRemoved) {
			File file = new File(extractDir,fileName);
			if (file.exists()) {
				logger.debug("File is excluded:" + file.getName());
				file.delete();
			}
		}
	}


	/*
	 * @return - the directory containing the files ready for uploading to SRS
	 */
	public File readyInputFiles(File archive, String releaseCenter, String releaseDate, boolean includeExternalFiles) throws ProcessWorkflowException,
			IOException {

		File extractDir = extractAndConvertExportWithRF2FileNameFormat(archive, releaseCenter, releaseDate, includeExternalFiles);
		// Now rename files to make the import compatible
		renameFiles(extractDir, "sct2", "rel2");
		renameFiles(extractDir, "der2", "rel2");
		// PGW 17/12/15 As a one off we're receiving CTV3 and SNOMED IDs in the SimpleMap file because this
		// Data was received from Termmed. Strip this file for the moment.
		File simpleMapFile = new File(extractDir, "rel2_sRefset_SimpleMapDelta_INT_" + releaseDate + TXT);
		filterUnacceptableValues(simpleMapFile, REFSET_ID_COLUMN, ACCEPTABLE_SIMPLEMAP_VALUES);
		return extractDir;
	}


	private void enforceReleaseDate(File extractDir, String enforcedReleaseDate) throws ProcessWorkflowException {
		//Loop through all the files in the directory and change the release date if required
		for (File thisFile : extractDir.listFiles()) {
			if (thisFile.isFile()) {
				String thisReleaseDate = findDateInString(thisFile.getName(), true);
				if (thisReleaseDate != null && !thisReleaseDate.equals(enforcedReleaseDate)) {
					logger.debug("Modifying releaseDate in " + thisFile.getName() + " to " + enforcedReleaseDate);
					renameFile(extractDir, thisFile, thisReleaseDate, enforcedReleaseDate);
				}
			}
		}
	}

	private void mergeRefsets(File extractDir, String fileType, String countryNamespace,String releaseDate) throws IOException {
		// Loop through our map of refsets required, and see what contributing files we can match
		for (Map.Entry<String, RefsetCombiner> refset : refsetMap.entrySet()) {

			RefsetCombiner rc = refset.getValue();
			String combinedRefset = getFilename(rc.targetFilePattern, fileType, countryNamespace, releaseDate);
			// Now can we find any of the contributing files to add to that file?
			boolean isFirstContributor = true;
			for (String contributorPattern : rc.sourceFilePatterns) {
				String contributorFilename = getFilename(contributorPattern, fileType, countryNamespace, releaseDate);
				File contributorFile = new File(extractDir, contributorFilename);
				File combinedRefsetFile = new File(extractDir, combinedRefset);
				if (contributorFile.exists()) {
					List<String> fileLines = FileUtils.readLines(contributorFile, StandardCharsets.UTF_8);
					// Don't need the header line for any subsequent files
					if (!isFirstContributor) {
						fileLines.remove(0);
					}
					boolean append = !isFirstContributor;
					FileUtils.writeLines(combinedRefsetFile, CharEncoding.UTF_8, fileLines, LINE_ENDING, append);
					isFirstContributor = false;
					// Now we can delete the contributor so it doesn't get uploaded as another input file
					contributorFile.delete();
				}
			}
			if (isFirstContributor) {
				logger.warn("Failed to find any files to contribute to {}", combinedRefset);
			} else {
				logger.debug("Created combined refset {}", combinedRefset);
			}
		}
	}

	private String getFilename(String filenamePattern, String fileType, String countryNamespace,String date) {
		return filenamePattern.replace(FILE_TYPE_INSERT, fileType).replace(COUNTRY_OR_NAMSPACE,countryNamespace).replace(RELEASE_DATE_INSERT, date);
	}

	private void renameFiles(File targetDirectory, String find, String replace) {
		Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
				+ " must be a directory in order to rename files from " + find + " to " + replace);
		for (File thisFile : targetDirectory.listFiles()) {
			renameFile(targetDirectory, thisFile, find, replace);
		}
	}

	private void renameFile(File parentDir, File thisFile, String find, String replace) {
		if (thisFile.exists() && !thisFile.isDirectory()) {
			String currentName = thisFile.getName();
			String newName = currentName.replace(find, replace);
			if (!newName.equals(currentName)) {
				File newFile = new File(parentDir, newName);
				thisFile.renameTo(newFile);
			}
		}
	}

	/**
	 * @param targetDirectory
	 * @param find
	 * @param replace
	 * @param columnNum
	 *            searched for term must match in this column
	 * @throws IOException
	 */
	protected void replaceInFiles(File targetDirectory, String find, String replace, int columnNum) throws IOException {
		Assert.isTrue(targetDirectory.isDirectory(), targetDirectory.getAbsolutePath()
				+ " must be a directory in order to replace text from " + find + " to " + replace);

		logger.info("Replacing {} with {} in target directory {}", find, replace, targetDirectory);
		for (File thisFile : targetDirectory.listFiles()) {
			if (thisFile.exists() && !thisFile.isDirectory()) {
				List<String> oldLines = FileUtils.readLines(thisFile, StandardCharsets.UTF_8);
				List<String> newLines = new ArrayList<String>();
				for (String thisLine : oldLines) {
					String[] columns = thisLine.split("\t");
					if (columns.length > columnNum && columns[columnNum].equals(find)) {
						thisLine = thisLine.replaceFirst(find, replace); // Would be more generic to rebuild from columns
					}
					newLines.add(thisLine);
				}
				FileUtils.writeLines(thisFile, CharEncoding.UTF_8, newLines, LINE_ENDING);
			}
		}
	}

	/*
	 * Creates a file containing all the rows which have "mustMatch" in columnNum. Plus the header row.
	 */
	protected void createSubsetFile(File source, File target, int columnNum, String mustMatch, boolean removeFromOriginal,
			boolean removeId)
			throws IOException {
		if (source.exists() && !source.isDirectory()) {
			logger.debug("Creating {} as a subset of {} and {} rows in original.", target, source, (removeFromOriginal ? "removing"
					: "leaving"));
			List<String> allLines = FileUtils.readLines(source, StandardCharsets.UTF_8);
			List<String> newLines = new ArrayList<String>();
			List<String> remainingLines = new ArrayList<String>();
			int lineCount = 1;
			for (String thisLine : allLines) {
				String[] columns = thisLine.split("\t");
				if (lineCount == 1 || (columns.length > columnNum && columns[columnNum].equals(mustMatch))) {
					// Are we wiping out the Id (column index 0) before writing?
					if (removeId && lineCount != 1) {
						columns[0] = "";
						String lineWithIDRemoved = StringUtils.join(columns, "\t");
						newLines.add(lineWithIDRemoved);
					} else {
						newLines.add(thisLine);
					}
					if (lineCount == 1) {
						remainingLines.add(thisLine);
					}
				} else {
					remainingLines.add(thisLine);
				}
				lineCount++;
			}
			FileUtils.writeLines(target, CharEncoding.UTF_8, newLines, LINE_ENDING);
			if (removeFromOriginal) {
				FileUtils.writeLines(source, CharEncoding.UTF_8, remainingLines, LINE_ENDING);
			}
		} else {
			logger.warn("Did not find file {} needed to create subset {}", source, target);
		}
	}

	/*
	 * Creates a file containing all the rows which have "mustMatch" in columnNum. Plus the header row.
	 */
	protected void filterUnacceptableValues(File target, int columnNum, Set<String> acceptableValues) throws IOException {
		if (target.exists() && !target.isDirectory()) {
			logger.debug("Filtering unacceptable values from " + target.getAbsolutePath());
			List<String> allLines = FileUtils.readLines(target, StandardCharsets.UTF_8);
			List<String> acceptableLines = new ArrayList<String>();
			int lineCount = 1;
			for (String thisLine : allLines) {
				String[] columns = thisLine.split("\t");
				if (lineCount == 1 || (columns.length > columnNum && acceptableValues.contains(columns[columnNum]))) {
					acceptableLines.add(thisLine);
				}
				lineCount++;
			}
			FileUtils.writeLines(target, CharEncoding.UTF_8, acceptableLines, LINE_ENDING);
		} else {
			logger.warn("Did not find file {} needed to filter out unacceptable values", target);
		}
	}

	private void stripAllExceptHeader(File target) throws IOException {

		logger.debug("Stripping all but header from: " + target.getAbsolutePath());
		// Move target file out of the way for a moment so it can be recreated
		// with just the header row
		File tempFile = null;
		try {
			tempFile = new File(target.getParent(), target.getName() + ".delete");
			Files.move(target, tempFile);
		} catch (IOException e) {
			logger.warn("Failed to strip all but headers from " + target.getAbsolutePath() + " due to " + e.getMessage());
			return;
		}

		// Read the first line from the temp file and write back to the original file
		InputStream fis = new FileInputStream(tempFile);
		InputStreamReader isr = new InputStreamReader(fis, CharEncoding.UTF_8);
		BufferedReader br = new BufferedReader(isr);
		String header = br.readLine();
		FileUtils.writeStringToFile(target, header, CharEncoding.UTF_8);
		br.close();
		isr.close();
		fis.close();

		tempFile.delete();
	}

	public String recoverReleaseDate(File archive) throws ProcessWorkflowException, IOException {
		// Ensure that we have a valid archive
		if (!archive.isFile()) {
			throw new ProcessWorkflowException("Could not open supplied archive: " + archive.getAbsolutePath());
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry ze = zis.getNextEntry();
		try {
			while (ze != null) {
				if (!ze.isDirectory()) {
					return findDateInString(ze.getName(), false);
				}
				ze = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
		}
		throw new ProcessWorkflowException("No files found in archive: " + archive.getAbsolutePath());
	}

	public String findDateInString(String str, boolean optional) throws ProcessWorkflowException {
		Matcher dateMatcher = Pattern.compile("(\\d{8})").matcher(str);
		if (dateMatcher.find()) {
			return dateMatcher.group();
		} else {
			if (optional) {
				logger.warn("Did not find a date in: " + str);
			} else {
				throw new ProcessWorkflowException("Unable to determine date from " + str);
			}
		}
		return null;
	}

	public void unzipFlat(File archive, File targetDir) throws ProcessWorkflowException, IOException {

		if (!targetDir.exists() || !targetDir.isDirectory()) {
			throw new ProcessWorkflowException(targetDir + " is not a viable directory in which to extract archive");
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry ze = zis.getNextEntry();
		try {
			while (ze != null) {
				if (!ze.isDirectory()) {
					Path p = Paths.get(ze.getName());
					String extractedFileName = p.getFileName().toString();
					File extractedFile = new File(targetDir, extractedFileName);
					OutputStream out = new FileOutputStream(extractedFile);
					IOUtils.copy(zis, out);
					IOUtils.closeQuietly(out);
				}
				ze = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
		}
	}

	private void includeExternallyMaintainedFiles(File extractDir,String releaseCenter,String targetReleaseDate) throws IOException {
		FileHelper s3 = new FileHelper(this.refsetBucket, s3Client);

		// Recover all files in the folder ready for the next release
		logger.debug("Recovering External Files from {}/{}/{}", this.refsetBucket, releaseCenter,targetReleaseDate);
		String directoryPath = releaseCenter + "/" + targetReleaseDate;
		List<String> externalFiles = s3.listFiles(directoryPath);

		for (String externalFile : externalFiles) {
			// The current directory is also listed
			if (externalFile != null && externalFile.equals("/"))
				continue;
			InputStream fileStream = null;
			try {
				// Note that filename already contains directory separator, so append directly
				fileStream = s3.getFileStream(directoryPath + externalFile);
				File localExternalFile = new File(extractDir, externalFile);
				logger.debug("Pulling in external file to {}, replacing any existing", localExternalFile.getAbsolutePath());
				java.nio.file.Files.copy(fileStream, localExternalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				logger.error("Failed to pull external file from S3: {}/{}/{}", releaseCenter, targetReleaseDate, externalFile, e);
			} finally {
				IOUtils.closeQuietly(fileStream);
			}
		}

	}

}
