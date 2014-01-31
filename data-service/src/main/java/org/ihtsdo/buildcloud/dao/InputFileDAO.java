package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.InputFile;

public interface InputFileDAO {
	InputFile find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildBusinessKey, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId);
}
