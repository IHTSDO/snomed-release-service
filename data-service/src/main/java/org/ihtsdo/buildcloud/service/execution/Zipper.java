package org.ihtsdo.buildcloud.service.execution;

import com.google.common.io.Files;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Zipper {
	
	private ExecutionDAO executionDAO;
	
	private Execution execution;
	
	private Package pkg;
	
	private InputStream manifestInputSteam;
	
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
	
	public void loadManifest() throws JAXBException, ResourceNotFoundException{
		//Get the manifest file as an input stream
		manifestInputSteam = executionDAO.getManifestStream(execution, pkg);
		
		if ( manifestInputSteam == null) {
			throw new ResourceNotFoundException ("Failed to load manifest due to null inputstream");
		}
		//Load the manifest file xml into a java object hierarchy
		JAXBContext jc = JAXBContext.newInstance( "org.ihtsdo.buildcloud.manifest");
		Unmarshaller um = jc.createUnmarshaller();
		manifestListing = um.unmarshal(new StreamSource(manifestInputSteam), ListingType.class).getValue();
		//Zip file name is the same as the root folder defined in manifest, with .zip appended
		rootFolder = manifestListing.getFolder();
		
		if (rootFolder == null) {
			throw new ResourceNotFoundException ("Failed to recover root folder from manifest.  Ensure the root element is named 'listing' " 
												+ "and it has a namespace of  xmlns=\"http://release.ihtsdo.org/manifest/1.0.0\" ");
		}
		isInitialised = true;
	}
	
	private File createArchive() throws IOException {
		
		assert(isInitialised);  //Would be a coding error if this tripped
		

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
	
	private void walkFolders(FolderType f, ZipOutputStream zos, String parentPath) throws IOException {
		//Create an entry for this folder
		String thisFolder = parentPath + f.getName() + PATH_CHAR;
		zos.putNextEntry(new ZipEntry(thisFolder));
		
		//Pull down and compress any child files
		for(FileType file : f.getFile()) {
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
