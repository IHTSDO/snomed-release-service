package org.ihtsdo.buildcloud.service.execution;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;


public class Zipper {
	
	private ExecutionDAO executionDAO;
	
	private Execution execution;
	
	private Package pkg;
	
	private ListingType manifestListing;
	
	private static final String PATH_CHAR = "/";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Zipper.class);
	
	public Zipper (Execution execution, Package pkg, ExecutionDAO executionDAO) {
		this.execution = execution;
		this.pkg = pkg;
		this.executionDAO = executionDAO;
	}
	
	public File createZipFile() throws JAXBException, IOException {
		//Get the manifest file as an input stream
		InputStream is = executionDAO.getManifestStream(execution, pkg);
		loadManifest(is);
		File zipFile = createArchive();
		return zipFile;
	}
	
	private void loadManifest(InputStream is) throws JAXBException{
		//Load the manifest file xml into a java object hierarchy
		JAXBContext jc = JAXBContext.newInstance( "org.ihtsdo.buildcloud.manifest");
		Unmarshaller um = jc.createUnmarshaller();
		manifestListing = um.unmarshal(new StreamSource(is),ListingType.class).getValue();
	}
	
	private File createArchive() throws IOException {
		
		String targetPath = Files.createTempDir().getAbsolutePath();
		
		//Zip file name is the same as the root folder defined in manifest, with .zip appended
		FolderType rootFolder = manifestListing.getFolder();
		String zipLocation = targetPath + File.separator + rootFolder.getName() + ".zip";

		//Option to use Google's InputStreamFromOutputStream here to feed directly 
		//up to S3, but that would use another thread in parallel, so not without risk.
		//Simpler to write to local disk for now and upload when complete.
		
		File zipFile = new File(zipLocation);
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		walkFolders(rootFolder, zos, "");
		zos.close();
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
				if (is != null) {
					zos.putNextEntry(new ZipEntry(thisFolder + file.getName()));
					IOUtils.copy(is, zos);
					is.close();
				}
			} catch (Exception e) {
				//TODO We'll want to report missing files in the telemetry
				LOGGER.warn ("Manifest failed to find expected output file: " + file.getName());
			}
		}

		//Recurse through child folders
		for (FolderType childFolder : f.getFolder()) {
			walkFolders(childFolder, zos, thisFolder);
		}
	}

}
