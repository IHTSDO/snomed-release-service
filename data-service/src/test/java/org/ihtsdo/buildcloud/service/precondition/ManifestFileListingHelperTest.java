package org.ihtsdo.buildcloud.service.precondition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.junit.Test;

public class ManifestFileListingHelperTest {
    private static final String TXT_EXTENSION = ".txt";

    @Test
    public void testListAllFilesInOneFolder(){
	ListingType listingType = new ListingType();
	FolderType folder = createFolder("Root",0);
	int total=5;
	folder.getFile().addAll(createFileTypes(total));
	listingType.setFolder(folder);
	List<String> actual = ManifestFileListingHelper.listAllFiles(listingType);
	assertEquals("Total files expected:" + total, total, actual.size());
	assertFilesNames(actual);
	
    }

    private void assertFilesNames(List<String> actual) {
	for(String fileName:actual){
	    assertTrue("File names should contian .txt",fileName.endsWith(TXT_EXTENSION));
	}
    }

    @Test
    public void testOneRootFolderAndTwoSubFolders(){
	ListingType listingType = new ListingType();
	FolderType root = createFolder("Root",2);
	int total = 10;
	List<FileType> files = createFileTypes(total);
	root.getFile().addAll(files);
	for(FolderType subFolder:root.getFolder()){
	    subFolder.getFile().addAll(files);
	}
	listingType.setFolder(root);
	List<String> actual = ManifestFileListingHelper.listAllFiles(listingType);
	assertEquals("Total files expected:" + total*3, total*3, actual.size());
	assertFilesNames(actual);
    }
    @Test
    public void testNestedSubfolders(){
	FolderType root = createFolder("Root",1);
	int total = 10;
	List<FileType> files = createFileTypes(total);
	root.getFile().addAll(files);
	for(FolderType subFolder:root.getFolder()){
	    //root/sub1
	    subFolder.getFile().addAll(files);
	    FolderType nested1 = createFolder("nested1", 1);
	    //root/sub1/nest1
	    nested1.getFile().addAll(files);
	    for(FolderType nest1Sub:nested1.getFolder()){
		//root/sub1/nest1/sub/
		nest1Sub.getFile().addAll(files);
	    }
	    FolderType nested2 = createFolder("nested2", 1);
	    //root/sub1/nest1/sub/nest2
	    nested2.getFile().addAll(files);
	    
	    for(FolderType nest2Sub:nested2.getFolder()){
		//root/sub1/nest1/subOfNest1/nest2/subOfNest2
		nest2Sub.getFile().addAll(files);
	    }
	    nested1.getFolder().add(nested2);
	    subFolder.getFolder().add(nested1);
	    
	}
	ListingType listingType = new ListingType();
	listingType.setFolder(root);
	List<String> actual = ManifestFileListingHelper.listAllFiles(listingType);
	assertEquals("Total files expected:" + total*6, total*6, actual.size());
	assertFilesNames(actual);
    }

    @Test
    public void testNoFilesButFolder(){
	ListingType listingType = new ListingType();
	FolderType folder = createFolder("Root",2);
	listingType.setFolder(folder);
	List<String> actual = ManifestFileListingHelper.listAllFiles(listingType);
	int expected = 0;
	assertEquals("Total files expected:" + expected, expected, actual.size());
    }
    
    private FolderType createFolder(String folderName,int numberOfSubFolders) {
	FolderType folder = new FolderType();
	folder.setName(folderName);
	for(int i=0;i<numberOfSubFolders;i++){
	    FolderType subFolder = new FolderType();
	    subFolder.setName("subFolder"+i + "of" + folderName);
	    folder.getFolder().add(subFolder);
	}
	return folder;
    }
    
    private List<FileType> createFileTypes(int total){
	List<FileType> result = new ArrayList<>();
	for( int i=0;i<total;i++){
	    FileType file = new FileType();
	    file.setName("File" + i+ TXT_EXTENSION);
	    result.add(file);
	}
	return result;
    }
}
