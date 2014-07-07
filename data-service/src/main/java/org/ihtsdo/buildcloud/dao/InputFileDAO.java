package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Package;

import java.io.InputStream;
import java.util.List;

public interface InputFileDAO {
	
	InputStream getManifestStream(Package pkg);
	
	public List<String> listInputFilePaths(Package aPackage);

	String getManifestPath(Package aPackage);

	void putManifestFile(Package pkg, InputStream inputStream,
			String originalFilename, long fileSize);

	void deleteManifest(Package pkg);

	String getManifestPath(Package pkg, String filename);

}
