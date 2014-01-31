package org.ihtsdo.buildcloud.service;

import org.hibernate.Hibernate;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.InputFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class InputFileServiceImpl implements InputFileService {

	@Autowired
	private PackageDAO packageDAO;

	@Override
	public Set<InputFile> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String buildBusinessKey, String packageBusinessKey, String authenticatedId) {
		Set<InputFile> inputFiles = packageDAO.find(releaseCentreBusinessKey, extensionBusinessKey, productBusinessKey, buildBusinessKey, packageBusinessKey, authenticatedId).getInputFiles();
		Hibernate.initialize(inputFiles);
		return inputFiles;
	}
}
