package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.InputFileDAO;
import org.ihtsdo.buildcloud.dao.PackageDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.execution.ReleaseFileGenerator;
import org.ihtsdo.buildcloud.service.execution.ReplaceValueLineTransformation;
import org.ihtsdo.buildcloud.service.execution.StreamingFileTransformation;
import org.ihtsdo.buildcloud.service.execution.UUIDTransformation;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
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
	private ExecutionConfigurationJsonGenerator executionConfigurationJsonGenerator;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Override
	public Execution create(String buildCompositeKey, User authenticatedUser) throws IOException, BadConfigurationException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);

		if (build.getEffectiveTime() != null) {

			Date creationDate = new Date();


			Execution execution = new Execution(creationDate, build);

			// Copy all files from Build input directory to Execution input directory
			dao.copyAll(build, execution);

			// Create Build config export
			String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);

			// Persist export
			dao.save(execution, jsonConfig);

			return execution;
		} else {
			throw new BadConfigurationException("Build effective time must be set before an execution is created.");
		}
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
		
		//Easiest thing for iteration 1 is to process just the first package for a build
		Package pkg = execution.getBuild().getPackages().get(0);

		transformFiles(execution);
		
		//Convert Delta files to Full, Snapshot and delta release files
				ReleaseFileGenerator generator = new ReleaseFileGenerator( execution, dao );
				generator.generateReleaseFiles();

		try {
			Zipper zipper = new Zipper(execution, pkg, dao);
			File zip = zipper.createZipFile();
			dao.putOutputFile(execution, pkg, zip, "", true);
		} catch (JAXBException jbex) {
			//TODO Telemetry about failures, but will not prevent process from continuing
			LOGGER.error("Failure in Zip creation caused by JAXB.", jbex);
		} catch (NoSuchAlgorithmException nsaEx) {
			LOGGER.error("Failure in Zip creation caused by hashing algorithm.", nsaEx);
		} catch (Exception e) {
			LOGGER.error("Failure in Zip creation caused by ", e);
		}

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
		String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveTimeSnomedFormat();
		transformation.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveDateInSnomedFormat));
		transformation.addLineTransformation(new UUIDTransformation(0));

		// Iterate each execution package
		Map<String, Object> executionConfigMap = dao.loadConfigurationMap(execution);
		Map<String, Object> build = (Map<String, Object>) executionConfigMap.get("build");
		List<Map<String, String>> packages = (List<Map<String, String>>) build.get("packages");
		for (Map<String, String> aPackage : packages) {
			String packageBusinessKey = aPackage.get("id");
			LOGGER.info("Transforming files in execution {}, package {}", execution.getId(), packageBusinessKey);

			// Iterate each execution input file
			List<String> executionInputFilePaths = dao.listInputFilePaths(execution, packageBusinessKey);

			for (String relativeFilePath : executionInputFilePaths) {

				// Transform all txt files. We are assuming they are all RefSet files for this Epic.
				if (relativeFilePath.endsWith(".txt")) {
					InputStream executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, relativeFilePath);
					OutputStream executionTransformedOutputStream = dao.getExecutionTransformedFileOutputStream(execution, packageBusinessKey, relativeFilePath);
					transformation.transformFile(executionInputFileInputStream, executionTransformedOutputStream);
				} else {
					dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
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
	public void putOutputFile(String buildCompositeKey, String executionId, String filePath, InputStream inputStream, Long size, User authenticatedUser) {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		dao.putOutputFile(execution, filePath, inputStream, size);
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
