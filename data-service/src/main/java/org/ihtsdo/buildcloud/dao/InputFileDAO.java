package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.InputFile;

public interface InputFileDAO {
	InputFile find(Long buildId, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId);
}
