package org.ihtsdo.buildcloud.service.execution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.manifest.*;
import org.ihtsdo.buildcloud.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.io.Files;

public class Zipper {
	
	@Autowired
	private FileService fileService;
	
	private Package pkg;
	private ListingType manifestListing;
	private String zipName;
	
	private static final String PATH_CHAR = "/";
	
	//private static final Logger LOGGER = LoggerFactory.getLogger(Zipper.class);
	
	public Zipper (Package pkg, String zipName, FileService fs) {
		this.pkg = pkg;
		this.zipName = zipName;
		this.fileService = fs;
	}
	
	public File createZipFile() throws JAXBException, IOException {
		//Get the manifest file as an input stream
		InputStream is = fileService.getManifestStream(pkg);
		return createZipFile(is);
	}
	
	File createZipFile(InputStream is) throws JAXBException, IOException {
		loadManifest(is);
		File zipFile = createArchive();
		//uploadArchive(zipFile);
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
		String zipLocation = targetPath + File.separator + this.zipName;

		//Option to use Google's InputStreamFromOutputStream here to feed directly 
		//up to S3, but that would use another thread in parallel, so not without risk.
		//Simpler to write to local disk for now and upload when complete.
		File zipFile = new File(zipLocation);
		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		walkFolders(manifestListing.getFolder(), zos, "/");
		zos.close();
		return zipFile;
	}
	
	private void walkFolders(FolderType f, ZipOutputStream zos, String parentPath) throws IOException {
		//Create an entry for this folder
		String thisFolder = parentPath + f.getName() + PATH_CHAR;
		zos.putNextEntry(new ZipEntry(thisFolder));
		
		//Pull down and compress any child files
		for(FileType file : f.getFile()) {
			InputStream is = fileService.getFileInputStream(pkg, file.getName());
			if (is != null) {
				zos.putNextEntry(new ZipEntry(thisFolder + file.getName()));
				IOUtils.copy(is, zos);
				is.close();
			}
		}
		
		//Recurse through child folders
		for (FolderType childFolder : f.getFolder()) {
			walkFolders(childFolder, zos, thisFolder);
		}
	}


}
