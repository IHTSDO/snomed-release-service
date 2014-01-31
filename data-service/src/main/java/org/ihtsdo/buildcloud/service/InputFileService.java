package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.InputFile;

import java.util.Set;

public interface InputFileService {

	Set<InputFile> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey,
						   String buildBusinessKey, String packageBusinessKey, String authenticatedId);

}
