package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.InputFile;

import java.util.Set;

public interface InputFileService {

	Set<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, String authenticatedId);

	InputFile find(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId);
}
