package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.ihtsdo.buildcloud.service.maven.MavenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

	@Autowired
	private ExecutionDAO dao;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	@Autowired
	private MavenGenerator mavenGenerator;

	@Override
	public Execution create(String buildCompositeKey, String authenticatedId) throws IOException {
		Build build = getBuild(buildCompositeKey, authenticatedId);

		Date creationDate = new Date();

		Execution execution = new Execution(creationDate, build);

		// Create Build config export
		String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);

		// Persist export
		dao.save(execution, jsonConfig);

		return execution;
	}

	@Override
	public List<Execution> findAll(String buildCompositeKey, String authenticatedId) {
		Build build = getBuild(buildCompositeKey, authenticatedId);
		return dao.findAll(build);
	}

	@Override
	public Execution find(String buildCompositeKey, String executionId, String authenticatedId) {
		Build build = getBuild(buildCompositeKey, authenticatedId);
		return dao.find(build, executionId);
	}

	@Override
	public String loadConfiguration(String buildCompositeKey, String executionId, String authenticatedId) throws IOException {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedId);
		return dao.loadConfiguration(execution);
	}

	private Execution getExecution(String buildCompositeKey, String executionId, String authenticatedId) {
		Build build = getBuild(buildCompositeKey, authenticatedId);
		return dao.find(build, executionId);
	}

	@Override
	public Execution triggerBuild(String buildCompositeKey, String executionId, String authenticatedId) throws IOException {
		Date triggerDate = new Date();

		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedId);

		String executionConfiguration = dao.loadConfiguration(execution);

		// Generate poms from config export
		File buildScriptsTmpDirectory = mavenGenerator.generateBuildScripts(executionConfiguration);

		dao.saveBuildScripts(buildScriptsTmpDirectory, execution);

		// Queue the Execution for building
		dao.queueForBuilding(execution);

		return execution;
	}

	@Override
	public void streamBuildScriptsZip(String buildCompositeKey, String executionId, String authenticatedId, OutputStream outputStream) throws IOException {
		Execution execution = find(buildCompositeKey, executionId, authenticatedId);
		dao.streamBuildScriptsZip(execution, outputStream);
	}

	@Override
	public void saveOutputFile(String buildCompositeKey, String executionId, String filePath, InputStream inputStream, Long size, String authenticatedId) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedId);
		dao.saveOutputFile(execution, filePath, inputStream, size);
	}

	@Override
	public void updateStatus(String buildCompositeKey, String executionId, String statusString, String authenticatedId) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedId);
		Execution.Status status = Execution.Status.valueOf(statusString);
		dao.updateStatus(execution, status);
	}

	private Build getBuild(String buildCompositeKey, String authenticatedId) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return buildDAO.find(buildId, authenticatedId);
	}

}
