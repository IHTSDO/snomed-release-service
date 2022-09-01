package org.ihtsdo.buildcloud.core.service.validation.precondition;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.buildcloud.core.manifest.FileType;
import org.ihtsdo.buildcloud.core.manifest.FolderType;
import org.ihtsdo.buildcloud.core.manifest.ListingType;

public class ManifestFileListingHelper {

	public static List<String> listAllFiles(ListingType listingType) {
		FolderType rootFolder = listingType.getFolder();
		List<String> result = new ArrayList<>();
		getFilesFromCurrentAndSubFolders(rootFolder, result, null);
		return result;
	}

	public static List<String> listAllFiles(ListingType listingType, String source) {
		FolderType rootFolder = listingType.getFolder();
		List<String> result = new ArrayList<>();
		getFilesFromCurrentAndSubFolders(rootFolder, result, source);
		return result;
	}

	public static List<String> listAllFiles(FolderType folderType, String source) {
		List<String> result = new ArrayList<>();
		getFilesFromCurrentAndSubFolders(folderType, result, source);
		return result;
	}

	public static  List<String> getFilesByFolderName(ListingType listingType, String folderName) {
		List<String> result = new ArrayList<>();
		List<FolderType> folderTypes = listingType.getFolder().getFolder();
		for (FolderType folderType : folderTypes) {
			if (folderType.getName().equals(folderName)) {
				getFilesFromCurrentAndSubFolders(folderType, result, null);
				break;
			}
		}

		return  result;
	}

	private static void getFilesFromCurrentAndSubFolders(FolderType folder, List<String> filesList, String source) {
		if (folder != null) {
			if (folder.getFile() != null) {
				for (FileType fileType : folder.getFile()) {
					if (source == null) {
						filesList.add(fileType.getName());
					} else if (fileType.getSources() != null){
						for (String s : fileType.getSources().getSource()) {
							if (source.equals(s)) {
								filesList.add(fileType.getName());
								break;
							}
						}
					}

				}
			}
			if (folder.getFolder() != null) {
				for (FolderType subFolder : folder.getFolder()) {
					getFilesFromCurrentAndSubFolders(subFolder, filesList, source);
				}
			}
		}
	}
}
