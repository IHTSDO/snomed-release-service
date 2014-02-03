package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private InputFileDAO inputFileDAO;

	@Autowired
	private PackageDAO packageDAO;

	@Override
	public Set<InputFile> findAll(String buildCompositeKey, String packageBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Set<InputFile> inputFiles = packageDAO.find(buildId, packageBusinessKey, authenticatedId).getInputFiles();
		Hibernate.initialize(inputFiles);
		return inputFiles;
	}

	@Override
	public InputFile find(String buildCompositeKey, String packageBusinessKey, String inputFileBusinessKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return inputFileDAO.find(buildId, packageBusinessKey, inputFileBusinessKey, authenticatedId);
	}
}
