package org.ihtsdo.buildcloud.service.execution;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.file.ManifestXmlFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;


public class Zipper {

	private final ExecutionDAO executionDAO;

	private final Execution execution;

	private final Package pkg;

	private ListingType manifestListing;

	private static final String PATH_CHAR = "/";

	private boolean isInitialised = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(Zipper.class);

	private FolderType rootFolder;

	public FolderType getRootFolder() {
		return rootFolder;
	}

	private static final int BUFFER_SIZE = 64 * 1024;

	public Zipper(Execution execution, Package pkg, ExecutionDAO executionDAO) {
		this.execution = execution;
		this.pkg = pkg;
		this.executionDAO = executionDAO;
	}

	public File createZipFile() throws JAXBException, IOException, ResourceNotFoundException {
		loadManifest();
		File zipFile = createArchive();
		return zipFile;
	}

	public void loadManifest() throws JAXBException, ResourceNotFoundException, IOException {
	    //Get the manifest file as an input stream
	   try (InputStream manifestInputSteam = executionDAO.getManifestStream(execution, pkg))
	   {
	       ManifestXmlFileParser parser = new ManifestXmlFileParser();
	       manifestListing = parser.parse(manifestInputSteam);
	   }
	    //Zip file name is the same as the root folder defined in manifest, with .zip appended
	    rootFolder = manifestListing.getFolder();
	    isInitialised = true;
	}

	private File createArchive() throws IOException {

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
		walkFolders(rootFolder, zos, "");
		zos.close();
		LOGGER.debug("Finished: Zipping file structure {}", rootFolder.getName());
		return zipFile;
	}

	private void walkFolders(final FolderType f, final ZipOutputStream zos, final String parentPath) throws IOException {
		//Create an entry for this folder
		String thisFolder = parentPath + f.getName() + PATH_CHAR;
		zos.putNextEntry(new ZipEntry(thisFolder));

		//Pull down and compress any child files
		for (FileType file : f.getFile()) {
			try {
				InputStream is = executionDAO.getOutputFileInputStream(execution, pkg, file.getName());
				BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
				if (is != null) {
					try {

						zos.putNextEntry(new ZipEntry(thisFolder + file.getName()));
						IOUtils.copy(bis, zos);
					} finally {
						zos.closeEntry();
						is.close();
					}
				}
			} catch (Exception e) {
				//TODO We'll want to report missing files in the telemetry
				LOGGER.warn("Manifest failed to find expected output file: " + file.getName());
			}
		}

		//Recurse through child folders
		for (FolderType childFolder : f.getFolder()) {
			walkFolders(childFolder, zos, thisFolder);
		}
	}

}
