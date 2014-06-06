package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.execution.ReplaceValueLineTransformation;
import org.ihtsdo.buildcloud.service.execution.StreamingFileTransformation;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

	@Autowired
	private ExecutionDAO dao;
	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private FileService fileService;

	@Autowired
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Override
	public Execution create(String buildCompositeKey, User authenticatedUser) throws IOException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);

		Date creationDate = new Date();

		Execution execution = new Execution(creationDate, build);

		// Copy all files from Build input directory to Execution input directory
		fileService.copyAll(build, execution);

		// Create Build config export
		String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);

		// Persist export
		dao.save(execution, jsonConfig);

		return execution;
	}

	@Override
	public List<Execution> findAll(String buildCompositeKey, User authenticatedUser) {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		return dao.findAll(build);
	}

	@Override
	public Execution find(String buildCompositeKey, String executionId, User authenticatedUser) {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		return dao.find(build, executionId);
	}

	@Override
	public String loadConfiguration(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		return dao.loadConfiguration(execution);
	}

	@Override
	public Execution triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		transformFiles(execution);
		return execution;
	}

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 * @param execution
	 * @throws IOException
	 */
	private void transformFiles(Execution execution) throws IOException {
		StreamingFileTransformation transformation = new StreamingFileTransformation();

		// Add streaming transformation of effectiveDate
		String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveDateSnomedFormat();
		transformation.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveDateInSnomedFormat));

		// Iterate each execution package
		Map<String, Object> executionConfigMap = dao.loadConfigurationMap(execution);
		Map<String, Object> build = (Map<String, Object>) executionConfigMap.get("build");
		List<Map<String, String>> packages = (List<Map<String, String>>) build.get("packages");
		for (Map<String, String> aPackage : packages) {
			String packageBusinessKey = aPackage.get("id");
			LOGGER.info("Transforming files in execution {}, package {}", execution.getId(), packageBusinessKey);

			// Iterate each execution input file
			List<String> executionInputFilePaths = fileService.listInputFilePaths(execution, packageBusinessKey);

			for (String relativeFilePath : executionInputFilePaths) {

				// Transform all txt files. We are assuming they are all RefSet files for this Epic.
				if (relativeFilePath.endsWith(".txt")) {
					InputStream executionInputFileInputStream = fileService.getExecutionInputFileStream(execution, packageBusinessKey, relativeFilePath);
					OutputStream executionOutputFileOutputStream = fileService.getExecutionOutputFileOutputStream(execution, packageBusinessKey, relativeFilePath);
					transformation.transformFile(executionInputFileInputStream, executionOutputFileOutputStream);
				} else {
					fileService.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
				}
			}
		}
	}

	@Override
	public void streamBuildScriptsZip(String buildCompositeKey, String executionId, User authenticatedUser, OutputStream outputStream) throws IOException {
		Execution execution = find(buildCompositeKey, executionId, authenticatedUser);
		dao.streamBuildScriptsZip(execution, outputStream);
	}

	@Override
	public void saveOutputFile(String buildCompositeKey, String executionId, String filePath, InputStream inputStream, Long size, User authenticatedUser) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		dao.saveOutputFile(execution, filePath, inputStream, size);
	}

	@Override
	public void updateStatus(String buildCompositeKey, String executionId, String statusString, User authenticatedUser) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		Execution.Status status = Execution.Status.valueOf(statusString);
		dao.updateStatus(execution, status);
	}

	@Override
	public InputStream getOutputFile(String buildCompositeKey, String executionId, String filePath, User authenticatedUser) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		return dao.getOutputFile(execution, filePath);
	}

	private Execution getExecution(String buildCompositeKey, String executionId, User authenticatedUser) {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		return dao.find(build, executionId);
	}

	private Build getBuild(String buildCompositeKey, User authenticatedUser) {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		return buildDAO.find(buildId, authenticatedUser);
	}

}
