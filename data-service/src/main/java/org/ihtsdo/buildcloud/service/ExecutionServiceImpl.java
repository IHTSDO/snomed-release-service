package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ConfigJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;

@Service
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

	@Autowired
	private ExecutionDAO dao;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ConfigJsonMapper configJsonMapper;

	@Override
	public Execution create(String buildCompositeKey, String authenticatedId) throws IOException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		Build build = buildDAO.find(buildId, authenticatedId);

		Date creationDate = new Date();

		// Create Build config export
		String jsonConfig = configJsonMapper.getJsonConfig(build);

		Execution execution = new Execution(creationDate, jsonConfig, build);

		// Persist export
		dao.save(execution);

		return execution;
	}

}
