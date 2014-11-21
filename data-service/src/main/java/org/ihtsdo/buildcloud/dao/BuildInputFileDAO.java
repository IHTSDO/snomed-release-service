package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;

import java.io.InputStream;
import java.util.List;

public interface BuildInputFileDAO {

	InputStream getManifestStream(Build build);

	public List<String> listRelativeInputFilePaths(Build build);

	String getManifestPath(Build build);

	void putManifestFile(Build build, InputStream inputStream,
			String originalFilename, long fileSize);

	void deleteManifest(Build build);

	String getKnownManifestPath(Build build, String filename);

}
