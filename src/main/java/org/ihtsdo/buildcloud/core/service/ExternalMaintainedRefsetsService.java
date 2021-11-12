package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface ExternalMaintainedRefsetsService {

	void putFile(File file, String target) throws BusinessServiceException;

	InputStream getFileStream(String filePath);

	List <String> listFiles(String directoryPath);
}
