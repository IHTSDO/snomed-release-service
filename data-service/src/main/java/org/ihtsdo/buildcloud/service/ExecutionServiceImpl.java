package org.ihtsdo.buildcloud.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import static org.ihtsdo.buildcloud.service.execution.RF2Constants.*;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Execution.Status;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.BadConfigurationException;
import org.ihtsdo.buildcloud.service.exception.NamingConflictException;
import org.ihtsdo.buildcloud.service.exception.ResourceNotFoundException;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.ReleaseFileGenerator;
import org.ihtsdo.buildcloud.service.execution.ReleaseFileGeneratorFactory;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.ihtsdo.buildcloud.service.execution.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.execution.transform.TransformationException;
import org.ihtsdo.buildcloud.service.execution.transform.TransformationService;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

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
	private PreconditionManager preconditionManager;

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private ReadmeGenerator readmeGenerator;

	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private TransformationService transformationService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Override
	public Execution create(String buildCompositeKey, User authenticatedUser) throws IOException, BadConfigurationException, NamingConflictException, ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);

		if (build.getEffectiveTime() != null) {

			Date creationDate = new Date();
			
			//Do we already have an execution for that date?
			Execution existingExecution = getExecution(build, creationDate);
			if (existingExecution != null) {
				throw new EntityAlreadyExistsException("An Execution for build " + buildCompositeKey + " already exists at timestamp " + creationDate);
			}

			Execution execution = new Execution(creationDate, build);

			// Copy all files from Build input and manifest directory to Execution input and manifest directory
			dao.copyAll(build, execution);
			
			//Perform Pre-condition testing (loops through each package)
			runPreconditionChecks(execution);

			// Create Build config export
			String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);

			// Persist export
			dao.save(execution, jsonConfig);

			return execution;
		} else {
			throw new BadConfigurationException("Build effective time must be set before an execution is created.");
		}
	}

	private void runPreconditionChecks(Execution execution) {
		Map<String, List<PreConditionCheckReport>> preConditionReports = preconditionManager.runPreconditionChecks(execution);
		execution.setPreConditionCheckReports(preConditionReports);
		//analyze report to check whether there is fatal error for all packages
		int fatalCountByPkg = 0;
		for (String pkgName : preConditionReports.keySet()) {
		    for (PreConditionCheckReport report : preConditionReports.get(pkgName)) {
				if (report.getResult() == State.FATAL) {
					fatalCountByPkg++;
					break;
				}
			}
		}
		//need to alert release manager as all packages have fatal pre-condition check error.
		if (fatalCountByPkg > 0 && fatalCountByPkg == execution.getBuild().getPackages().size()) {
		    execution.setStatus(Status.FAILED_PRE_CONDITIONS);
		}
	}

	@Override
	public List<Execution> findAll(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		if (build == null) {
			throw new ResourceNotFoundException ("Unable to find build: " +  buildCompositeKey);
		}

		return dao.findAll(build);
	}

	@Override
	public Execution find(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		
		if (build == null) {
			throw new ResourceNotFoundException ("Unable to find build: " +  buildCompositeKey);
		}

		return dao.find(build, executionId);
	}

	@Override
	public String loadConfiguration(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.loadConfiguration(execution);
	}

	@Override
	public List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		return getExecutionPackages(buildCompositeKey, executionId, null, authenticatedUser);
	}

	@Override
	public ExecutionPackageDTO getExecutionPackage(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		List<ExecutionPackageDTO> executionPackages = getExecutionPackages(buildCompositeKey, executionId, packageId, authenticatedUser);
		return !executionPackages.isEmpty() ? executionPackages.iterator().next() : null;
	}

	private List<ExecutionPackageDTO> getExecutionPackages(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		List<ExecutionPackageDTO> executionPackageDTOs = new ArrayList<>();
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		Map<String, Object> stringObjectMap = dao.loadConfigurationMap(execution);
		Map<String, Object> build = (Map<String, Object>) stringObjectMap.get("build");
		List<Map<String, Object>> packages = (List<Map<String, Object>>) build.get("packages");
		for (Map<String, Object> aPackage : packages) {
			String id = (String) aPackage.get("id");
			if (packageId == null || packageId.equals(id)) {
				executionPackageDTOs.add(new ExecutionPackageDTO(id, (String) aPackage.get("name")));
			}
		}
		return executionPackageDTOs;
	}

	@Override
	public Map<String, Object> triggerBuild(String buildCompositeKey, String executionId, User authenticatedUser) throws Exception {
		
		Map<String, Object> results = new HashMap<>();
		Map<String, Object> packageResults = new HashMap<>();
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		
		results.put("Execution", execution);
		results.put("PackageResults", packageResults);
		
		//We can only trigger a build for a build at status Execution.Status.BEFORE_TRIGGER
		dao.assertStatus(execution, Execution.Status.BEFORE_TRIGGER);
		
		dao.updateStatus(execution, Execution.Status.BUILDING);
		
		//Run transformation on each of our packages in turn.
		//TODO Multithreading opportunity here!
		Set<Package> packages = execution.getBuild().getPackages();
		for (Package pkg : packages) {
			String pkgResult = "pass";
			String msg = "Process completed successfully";
			try {
				executePackage(execution, pkg);
			} catch(Exception e) {
				//Each package could fail independently, record telemetry and move on to next package
				pkgResult = "fail";
				msg = "Failure while processing package " 
						+ pkg.getBusinessKey() 
						+ " due to " 
						+ (e.getMessage() != null ? e.getMessage() : e.getClass().getName());
				LOGGER.warn(msg, e);
			}
			Map<String, Object> thisResult = new HashMap<>();
			thisResult.put("status", pkgResult);
			thisResult.put("message", msg);
			packageResults.put(pkg.getBusinessKey(), thisResult);
		}
		
		dao.updateStatus(execution, Execution.Status.BUILT);

		return results;
	}
	
	private void executePackage(Execution execution, Package pkg) throws Exception {

		// TODO: Refactor: Process each input file in a separate thread where appropriate.

		//A sort of pre-Condition check we're going to ensure we have a manifest file before proceeding 
		InputStream manifestStream = dao.getManifestStream(execution, pkg);
		if (manifestStream == null) {
			throw new BadConfigurationException("Failed to find valid manifest file.");
		} else {
			manifestStream.close();
		}

		if (pkg.isJustPackage()) {
			copyFilesForJustPackaging(execution, pkg);
		} else {
			Map<String, TableSchema> inputFileSchemaMap = getInputFileSchemaMap(execution, pkg);
			transformationService.transformFiles(execution, pkg, inputFileSchemaMap);
			Map<String, TableSchema> transformedFileSchemaMap = getTransformedFileSchemaMap(inputFileSchemaMap);

			//Convert Delta files to Full, Snapshot and delta release files
			ReleaseFileGeneratorFactory generatorFactory = new ReleaseFileGeneratorFactory();
			ReleaseFileGenerator generator = generatorFactory.createReleaseFileGenerator(execution, pkg, transformedFileSchemaMap, dao);
			generator.generateReleaseFiles();
		}

		// Generate readme file
		generateReadmeFile(execution, pkg);

		try {
			Zipper zipper = new Zipper(execution, pkg, dao);
			File zip = zipper.createZipFile();
			dao.putOutputFile(execution, pkg, zip, "", true);
		} catch (Exception e)  {
			throw new Exception("Failure in Zip creation caused by " + e.getMessage(), e);
		}

	}

	private Map<String, TableSchema> getTransformedFileSchemaMap(Map<String, TableSchema> inputFileSchemaMap) {
		Map<String, TableSchema> transformedFileSchemaMap = new HashMap<>();
		for (TableSchema tableSchema : inputFileSchemaMap.values()) {
			if (tableSchema != null) {
				transformedFileSchemaMap.put(tableSchema.getFilename(), tableSchema);
			}
		}
		return transformedFileSchemaMap;
	}

	/**
	 * A streaming transformation of execution input files, creating execution output files.
	 * @param execution
	 * @throws TransformationException
	 * @throws IOException
	 */
	private void copyFilesForJustPackaging(Execution execution, Package pkg) {

		String packageBusinessKey = pkg.getBusinessKey();
		LOGGER.info("Just copying files in execution {}, package {} for packaging", execution.getId(), packageBusinessKey);

		// Iterate each execution input file
		List<String> executionInputFilePaths = dao.listInputFileNames(execution, packageBusinessKey);
		for (String relativeFilePath : executionInputFilePaths) {
				dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
		}
	}

	private Map<String, TableSchema> getInputFileSchemaMap(Execution execution, Package pkg) throws FileRecognitionException {
		List<String> executionInputFilePaths = dao.listInputFileNames(execution, pkg.getBusinessKey());
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		for (String executionInputFilePath : executionInputFilePaths) {
			TableSchema schemaBean = schemaFactory.createSchemaBean(FileUtils.getFilenameFromPath(executionInputFilePath));
			inputFileSchemaMap.put(executionInputFilePath, schemaBean);
		}
		return inputFileSchemaMap;
	}

	@Override
	public void updateStatus(String buildCompositeKey, String executionId, String statusString, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		
		Execution.Status status = Execution.Status.valueOf(statusString);
		dao.updateStatus(execution, status);
	}

	@Override
	public InputStream getOutputFile(String buildCompositeKey, String executionId, String packageId, String outputFilePath, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.getOutputFileStream(execution, packageId, outputFilePath);
	}

	@Override
	public List<String> getExecutionPackageOutputFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.listOutputFilePaths(execution, packageId);
	}

	@Override
	public InputStream getLogFile(String buildCompositeKey, String executionId, String packageId, String logFileName, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.getLogFileStream(execution, packageId, logFileName);
	}

	@Override
	public List<String> getExecutionPackageLogFilePaths(String buildCompositeKey, String executionId, String packageId, User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.listLogFilePaths(execution, packageId);
	}

	private Execution getExecutionOrThrow(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		if (execution == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, executionId);
			throw new ResourceNotFoundException("Unable to find execution: " +  item);
		}
		return execution;
	}

	private Execution getExecution(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		if (build == null) {
			throw new ResourceNotFoundException ("Unable to find build: " +  buildCompositeKey);
		}
		return dao.find(build, executionId);
	}
	
	private Execution getExecution(Build build, Date creationTime) {
		return dao.find(build, EntityHelper.formatAsIsoDateTime(creationTime));
	}
		

	private Build getBuild(String buildCompositeKey, User authenticatedUser) throws ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException ("Unable to find build: " + buildCompositeKey);
		}	
		return buildDAO.find(buildId, authenticatedUser);
	}

	private void generateReadmeFile(Execution execution, Package pkg) throws BadConfigurationException, JAXBException, IOException,
			ExecutionException, InterruptedException {

		Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
		InputStream manifestStream = dao.getManifestStream(execution, pkg);
		ListingType manifestListing = unmarshaller.unmarshal(new StreamSource(manifestStream), ListingType.class).getValue();

		String readmeFilename = null;
		if (manifestListing != null) {
			FolderType rootFolder = manifestListing.getFolder();
			if (rootFolder != null) {
				List<FileType> files = rootFolder.getFile();
				for (FileType file : files) {
					String filename = file.getName();
					if (file.getName().startsWith(README_FILENAME_PREFIX) && filename.endsWith(README_FILENAME_EXTENSION)) {
						readmeFilename = filename;
						break;
					}
				}
			}
		} else {
			LOGGER.warn("Can not generate readme, manifest listing is null.");
		}
		if (readmeFilename != null) {
			AsyncPipedStreamBean asyncPipedStreamBean = dao.getOutputFileOutputStream(execution, pkg.getBusinessKey(), readmeFilename);
			try (OutputStream readmeOutputStream = asyncPipedStreamBean.getOutputStream()) {
				readmeGenerator.generate(pkg.getReadmeHeader(), pkg.getReadmeEndDate(), manifestListing, readmeOutputStream);
				asyncPipedStreamBean.waitForFinish();
			}
		} else {
			LOGGER.warn("Can not generate readme, no file found in manifest root directory starting with '{}' and ending with '{}'",
					README_FILENAME_PREFIX, README_FILENAME_EXTENSION);
		}
	}

}
