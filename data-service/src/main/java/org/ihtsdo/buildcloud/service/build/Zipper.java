package org.ihtsdo.buildcloud.service.build;

import com.google.common.io.Files;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;


public class Zipper {

	private static final String DELTA = "delta";

	private static final String PATH_CHAR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(Zipper.class);

	private static final int BUFFER_SIZE = 64 * 1024;

	private static final String SNAPSHOT = "snapshot";

	private static final String FULL = "full";

	private final BuildDAO buildDAO;

	private final Build build;

	private ListingType manifestListing;

	private boolean isInitialised = false;

	private FolderType rootFolder;
	
	public Zipper(Build build, BuildDAO buildDAO) {
		this.build = build;
		this.buildDAO = buildDAO;
	}

	public File createZipFile(boolean deltaOnly) throws JAXBException, IOException, ResourceNotFoundException {
		loadManifest();
		File zipFile = createArchive(deltaOnly);
		return zipFile;
	}

	public void loadManifest() throws JAXBException, ResourceNotFoundException, IOException {
		// Get the manifest file as an input stream
	try (InputStream manifestInputSteam = buildDAO.getManifestStream(build)) {
		ManifestXmlFileParser parser = new ManifestXmlFileParser();
		manifestListing = parser.parse(manifestInputSteam);
	}
	// Zip file name is the same as the root folder defined in manifest, with .zip appended
	rootFolder = manifestListing.getFolder();
	isInitialised = true;
	}

	private File createArchive(boolean deltaOnly) throws IOException {

		assert (isInitialised);  //Would be a coding error if this tripped


		String targetPath = Files.createTempDir().getAbsolutePath();
		String zipLocation = targetPath + File.separator + rootFolder.getName() + ".zip";

		//Option to use Google's InputStreamFromOutputStream here to feed directly 
		//up to S3, but that would use another thread in parallel, so not without risk.
		//Simpler to write to local disk for now and upload when complete.

		LOGGER.debug("Start: Zipping file structure {}", rootFolder.getName());
		File zipFile = new File(zipLocation);
		FileOutputStream fos = new FileOutputStream(zipFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE);
		ZipOutputStream zos = new ZipOutputStream(bos);
		walkFolders(rootFolder, zos, "", deltaOnly);
		zos.close();
		LOGGER.debug("Finished: Zipping file structure {}", rootFolder.getName());
		return zipFile;
	}

	private void walkFolders(final FolderType f, final ZipOutputStream zos, final String parentPath, boolean deltaOnly) throws IOException {
		//Create an entry for this folder
		String thisFolder = parentPath + f.getName() + PATH_CHAR;
		zos.putNextEntry(new ZipEntry(thisFolder));

		//Pull down and compress any child files
		for (FileType file : f.getFile()) {
			String filename = file.getName();
			if (deltaOnly && !filename.toLowerCase().contains(DELTA)) {
				continue;
			}
			if (!Normalizer.isNormalized(filename,Form.NFC)) {
				filename = Normalizer.normalize(filename, Form.NFC);
				LOGGER.debug("NFC Normalized file name from manifest " + filename);
			}
			try (InputStream is = buildDAO.getOutputFileInputStream(build, filename)) {
				if (is != null) {
				BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
					try {
						zos.putNextEntry(new ZipEntry(thisFolder + filename));
						IOUtils.copy(bis, zos);
					} finally {
						zos.closeEntry();
						is.close();
					}
				} else {
					LOGGER.info(RF2Constants.DATA_PROBLEM + " Failed to find output file listed in manifest: " + filename);
				}
			}
		}

		//Recurse through child folders
		for (FolderType childFolder : f.getFolder()) {
			if (deltaOnly && (childFolder.getName().equalsIgnoreCase(SNAPSHOT) || childFolder.getName().equalsIgnoreCase(FULL))) {
				continue;
			}
			walkFolders(childFolder, zos, thisFolder, deltaOnly);
		}
	}

}
