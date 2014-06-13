package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.execution.*;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Override
	public Execution create(String buildCompositeKey, User authenticatedUser) throws IOException, BadConfigurationException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);

		if (build.getEffectiveTime() != null) {

			Date creationDate = new Date();

			Execution execution = new Execution(creationDate, build);

			// Copy all files from Build input and manifest directory to Execution input and manifest directory
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
	public Execution triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws Exception {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		
		//We can only trigger a build for a build at status Execution.Status.BEFORE_TRIGGER
		dao.assertStatus(execution, Execution.Status.BEFORE_TRIGGER);
		
		dao.updateStatus(execution, Execution.Status.BUILDING);
		
		//Run transformation on each of our packages in turn.
		//TODO Multithreading opportunity here!
		List<Package> packages = execution.getBuild().getPackages();
		for (Package pkg : packages) {
			try {
				executePackage(execution, pkg);
			} catch (Exception e) {
				//Each package could fail independently, record telemetry and move on to next package
				LOGGER.warn ("Failure while processing package {} due to: {}" , pkg.getBusinessKey(), e.getMessage());
			}
		}
		
		dao.updateStatus(execution, Execution.Status.BUILT);

		return execution;
	}
	
	private void executePackage(Execution execution, Package pkg) throws Exception {

		//A sort of pre-Condition check we're going to ensure we have a manifest file before proceeding 
		if (dao.getManifestStream(execution, pkg) == null) {
			throw new Exception ("Failed to find valid manifest file.");
		}
		
		transformFiles(execution, pkg);
		
		pause();
		
		//Convert Delta files to Full, Snapshot and delta release files
		ReleaseFileGenerator generator = new ReleaseFileGenerator( execution, pkg, dao );
		generator.generateReleaseFiles();
		
		pause();

		try {
			Zipper zipper = new Zipper(execution, pkg, dao);
			File zip = zipper.createZipFile();
			dao.putOutputFile(execution, pkg, zip, "", true);
		} catch (Exception e)  {
			throw (new Exception("Failure in Zip creation.", e));
		}

	}

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 * @param execution
	 * @throws IOException
	 */
	private void transformFiles(Execution execution, Package pkg) throws IOException {
		StreamingFileTransformation transformation = new StreamingFileTransformation();

		// Add streaming transformation of effectiveDate
		String effectiveDateInSnomedFormat = execution.getBuild().getEffectiveTimeSnomedFormat();
		transformation.addLineTransformation(new ReplaceValueLineTransformation(1, effectiveDateInSnomedFormat));
		transformation.addLineTransformation(new UUIDTransformation(0));

		String packageBusinessKey = pkg.getBusinessKey();
		LOGGER.info("Transforming files in execution {}, package {}", execution.getId(), packageBusinessKey);

		// Iterate each execution input file
		List<String> executionInputFilePaths = dao.listInputFilePaths(execution, packageBusinessKey);

		for (String relativeFilePath : executionInputFilePaths) {

			// Transform all txt files. We are assuming they are all RefSet files for this Epic.
			if (relativeFilePath.endsWith(".txt")) {
				InputStream executionInputFileInputStream = dao.getInputFileStream(execution, packageBusinessKey, relativeFilePath);
				OutputStream executionTransformedOutputStream = dao.getTransformedFileOutputStream(execution, packageBusinessKey, relativeFilePath);
				transformation.transformFile(executionInputFileInputStream, executionTransformedOutputStream);
			} else {
				dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
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
	
	/*
	 * Have a break, have a kitkat.
	 */
	private void pause() {
		try {
			Thread.sleep(1000);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
