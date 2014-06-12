package org.ihtsdo.buildcloud.dao;

import java.io.InputStream;
import java.util.List;

import org.ihtsdo.buildcloud.entity.Package;

public interface InputFileDAO {
	
	InputStream getManifestStream(Package pkg);
	
	public List<String> listInputFilePaths(Package aPackage);

}
