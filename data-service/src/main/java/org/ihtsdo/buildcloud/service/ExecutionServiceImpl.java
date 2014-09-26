package org.ihtsdo.buildcloud.service;

import org.apache.log4j.MDC;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.dao.io.AsyncPipedStreamBean;
import org.ihtsdo.buildcloud.dto.ExecutionPackageDTO;
import org.ihtsdo.buildcloud.entity.*;
import org.ihtsdo.buildcloud.entity.Execution.Status;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport.State;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import org.ihtsdo.buildcloud.manifest.FileType;
import org.ihtsdo.buildcloud.manifest.FolderType;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.exception.*;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;
import org.ihtsdo.buildcloud.service.execution.Rf2FileExportService;
import org.ihtsdo.buildcloud.service.execution.Zipper;
import org.ihtsdo.buildcloud.service.execution.readme.ReadmeGenerator;
import org.ihtsdo.buildcloud.service.execution.transform.TransformationService;
import org.ihtsdo.buildcloud.service.execution.transform.UUIDGenerator;
import org.ihtsdo.buildcloud.service.file.FileUtils;
import org.ihtsdo.buildcloud.service.helper.CompositeKeyHelper;
import org.ihtsdo.buildcloud.service.mapping.ExecutionConfigurationJsonGenerator;
import org.ihtsdo.buildcloud.service.precondition.PreconditionManager;
import org.ihtsdo.buildcloud.service.rvf.RVFClient;
import org.ihtsdo.snomed.util.rf2.schema.FileRecognitionException;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.ihtsdo.telemetry.client.TelemetryStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.ihtsdo.buildcloud.service.execution.RF2Constants.README_FILENAME_EXTENSION;
import static org.ihtsdo.buildcloud.service.execution.RF2Constants.README_FILENAME_PREFIX;

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
	private ReadmeGenerator readmeGenerator;

	@Autowired
	private SchemaFactory schemaFactory;

	@Autowired
	private TransformationService transformationService;

	@Autowired
	private Integer fileProcessingFailureMaxRetry;

	@Autowired
	private String releaseValidationFrameworkUrl;

	@Autowired
	private Boolean offlineMode;

	@Autowired
	private Boolean localRvf;

	@Autowired
	private UUIDGenerator uuidGenerator;

	@Autowired
	private RF2ClassifierService classifierService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceImpl.class);

	@Override
	public Execution create(final String buildCompositeKey, final User authenticatedUser) 
			throws IOException, BadConfigurationException, 
			NamingConflictException, ResourceNotFoundException, EntityAlreadyExistsException {
		Date creationDate = new Date();
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		if (build.getEffectiveTime() == null) {
			throw new BadConfigurationException("Build effective time must be set before an execution is created.");
		}
		Execution execution = null;
		try {
			synchronized (buildCompositeKey) {
				//Do we already have an execution for that date?
				Execution existingExecution = getExecution(build, creationDate);
				if (existingExecution != null) {
					throw new EntityAlreadyExistsException("An Execution for build " + buildCompositeKey + " already exists with execution id " + existingExecution.getId());
				}
				execution = new Execution(creationDate, build);
				// Create Build config export
				String jsonConfig = executionConfigurationJsonGenerator.getJsonConfig(execution);
				// save execution with config
				dao.save(execution, jsonConfig);
				MDC.put(MDC_EXECUTION_KEY, execution.getUniqueId());
				LOGGER.info("Created execution.", buildCompositeKey, execution.getId());
				// Copy all files from Build input and manifest directory to Execution input and manifest directory
				dao.copyAll(build, execution);
			}
			//Perform Pre-condition testing (loops through each package)
			Status preStatus = execution.getStatus();
			runPreconditionChecks(execution);
			Status newStatus = execution.getStatus();
			if (newStatus != preStatus) {
				dao.updateStatus(execution, newStatus);
			}
		} finally {
			MDC.remove(MDC_EXECUTION_KEY);
		}
		return execution;
	}

	private void runPreconditionChecks(final Execution execution) {
	    LOGGER.info("Start of Pre-condition checks");
		Map<String, List<PreConditionCheckReport>> preConditionReports = preconditionManager.runPreconditionChecks(execution);
		execution.setPreConditionCheckReports(preConditionReports);
		//analyze report to check whether there is fatal error for all packages
		int fatalCountByPkg = 0;
		for (String pkgName : preConditionReports.keySet()) {
			for (PreConditionCheckReport report : preConditionReports.get(pkgName)) {
				if (report.getResult() == State.FATAL) {
					fatalCountByPkg++;
					LOGGER.warn("Fatal error occurred during pre-condition check for package {}", pkgName);
					break;
				}
			}
		}
		//need to alert release manager as all packages have fatal pre-condition check error.
		if (fatalCountByPkg > 0 && fatalCountByPkg == execution.getBuild().getPackages().size()) {
			execution.setStatus(Status.FAILED_PRE_CONDITIONS);
			LOGGER.warn("Fatal error occurred for all packages during pre-condition checks and execution {} will be halted.", execution.getId());
		}
		LOGGER.info("End of Pre-condition checks");
	}

	@Override
	public List<Execution> findAllDesc(final String buildCompositeKey, final User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}

		return dao.findAllDesc(build);
	}

	@Override
	public Execution find(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);

		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}

		return dao.find(build, executionId);
	}

	@Override
	public String loadConfiguration(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.loadConfiguration(execution);
	}

	@Override
	public List<ExecutionPackageDTO> getExecutionPackages(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
		return getExecutionPackages(buildCompositeKey, executionId, null, authenticatedUser);
	}

	@Override
	public ExecutionPackageDTO getExecutionPackage(final String buildCompositeKey, final String executionId, final String packageId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
		List<ExecutionPackageDTO> executionPackages = getExecutionPackages(buildCompositeKey, executionId, packageId, authenticatedUser);
		return !executionPackages.isEmpty() ? executionPackages.iterator().next() : null;
	}

	@SuppressWarnings("unchecked")
	private List<ExecutionPackageDTO> getExecutionPackages(final String buildCompositeKey, final String executionId, final String packageId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
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
	public Execution triggerBuild(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws Exception {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		TelemetryStream.start(LOGGER, dao.getTelemetryExecutionLogFilePath(execution));
		try {
			LOGGER.info("Trigger build", buildCompositeKey, executionId);

			//We can only trigger a build for a build at status Execution.Status.BEFORE_TRIGGER
			dao.assertStatus(execution, Execution.Status.BEFORE_TRIGGER);

			dao.updateStatus(execution, Execution.Status.BUILDING);

			//Run transformation on each of our packages in turn.
			// TODO: Could multithread here, if there are enough resources (classifier and RF2 export will use a lot of memory).
			Set<Package> packages = execution.getBuild().getPackages();
			for (Package pkg : packages) {
				ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
				String pkgResult = "completed";
				String msg = "Process completed successfully";
				try {
					executePackage(execution, pkg); // This could add entries to the execution report also
				} catch (Exception e) {
					//Each package could fail independently, record telemetry and move on to next package
					pkgResult = "fail";
					msg = "Failure while processing package " + pkg.getBusinessKey() + " due to: "
							+ e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "");
					LOGGER.warn(msg, e);
				}
				report.add("Progress Status", pkgResult);
				report.add("Message", msg);
			}

			dao.persistReport(execution);

			dao.updateStatus(execution, Execution.Status.BUILT);
		} finally {
			TelemetryStream.finish(LOGGER);
		}

		return execution;
	}

	private void executePackage(final Execution execution, final Package pkg) throws Exception {

		LOGGER.info("Start executing package {}", pkg.getName());
		ExecutionPackageReport report = execution.getExecutionReport().getExecutionPackgeReport(pkg);
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

			//Convert Delta files to Full, Snapshot and delta release files
			Rf2FileExportService generator = new Rf2FileExportService(execution, pkg, dao, uuidGenerator, fileProcessingFailureMaxRetry);
			generator.generateReleaseFiles();

			// Run classifier to produce inferred relationships from stated relationships
			classifierService.generateInferredRelationships(execution, pkg, inputFileSchemaMap);
		}

		// Generate readme file
		generateReadmeFile(execution, pkg);

		File zipPackage;
		try {
			Zipper zipper = new Zipper(execution, pkg, dao);
			zipPackage = zipper.createZipFile();
			LOGGER.info("Start: Upload zipPackage file {}", zipPackage.getName());
			dao.putOutputFile(execution, pkg, zipPackage, "", true);
			LOGGER.info("Finish: Upload zipPackage file {}", zipPackage.getName());
		} catch (Exception e) {
			throw new Exception("Failure in Zip creation caused by " + e.getMessage(), e);
		}

		String rvfResultMsg = "RVF validation configured to not run.";
		String rvfStatus = "N/A";
		if (!offlineMode || localRvf) {
			try {
				rvfResultMsg = runRVFPostConditionCheck(zipPackage, execution, pkg.getBusinessKey());
				if (rvfResultMsg == null) {
					rvfStatus = "Completed without errors.";
				} else {
					rvfStatus = "Completed, errors detected.";
				}
			} catch (Exception e) {
				LOGGER.error("Failure during RVF Post Condition Testing", e);
				rvfStatus = "Processing Failed.";
				rvfResultMsg = "Failure due to: " + e.getLocalizedMessage();
			}
		}
		report.add("Post Validation Status", rvfStatus);
		report.add("RVF Test Failures", rvfResultMsg);
		LOGGER.info("End of executing package {}", pkg.getName());
	}

	private String runRVFPostConditionCheck(final File zipPackage, final Execution execution, final String pkgBusinessKey) throws IOException,
			PostConditionException {
	    	LOGGER.info("Run RVF post-condition check for zip file {}", zipPackage.getName());
		try (RVFClient rvfClient = new RVFClient(releaseValidationFrameworkUrl);
				AsyncPipedStreamBean logFileOutputStream = dao.getLogFileOutputStream(execution, pkgBusinessKey, "postcheck-rvf-"
						+ zipPackage.getName() + ".log")) {
			return rvfClient.checkOutputPackage(zipPackage, logFileOutputStream);
		}
	}

	private void copyFilesForJustPackaging(final Execution execution, final Package pkg) {

		String packageBusinessKey = pkg.getBusinessKey();
		LOGGER.info("Just copying files in execution {}, package {} for packaging", execution.getId(), packageBusinessKey);

		// Iterate each execution input file
		List<String> executionInputFilePaths = dao.listInputFileNames(execution, packageBusinessKey);
		for (String relativeFilePath : executionInputFilePaths) {
			dao.copyInputFileToOutputFile(execution, packageBusinessKey, relativeFilePath);
		}
	}

	private Map<String, TableSchema> getInputFileSchemaMap(final Execution execution, final Package pkg) throws FileRecognitionException {
		List<String> executionInputFilePaths = dao.listInputFileNames(execution, pkg.getBusinessKey());
		Map<String, TableSchema> inputFileSchemaMap = new HashMap<>();
		for (String executionInputFilePath : executionInputFilePaths) {
			TableSchema schemaBean = schemaFactory.createSchemaBean(FileUtils.getFilenameFromPath(executionInputFilePath));
			inputFileSchemaMap.put(executionInputFilePath, schemaBean);
		}
		return inputFileSchemaMap;
	}

	@Override
	public void updateStatus(final String buildCompositeKey, final String executionId, final String statusString, final User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);

		Execution.Status status = Execution.Status.valueOf(statusString);
		dao.updateStatus(execution, status);
	}

	@Override
	public InputStream getOutputFile(final String buildCompositeKey, final String executionId, final String packageId, final String outputFilePath, final User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.getOutputFileStream(execution, packageId, outputFilePath);
	}

	@Override
	public List<String> getExecutionPackageOutputFilePaths(final String buildCompositeKey, final String executionId, final String packageId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.listOutputFilePaths(execution, packageId);
	}

	@Override
	public InputStream getLogFile(final String buildCompositeKey, final String executionId, final String packageId, final String logFileName, final User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.getLogFileStream(execution, packageId, logFileName);
	}

	@Override
	public List<String> getExecutionPackageLogFilePaths(final String buildCompositeKey, final String executionId, final String packageId, final User authenticatedUser) throws IOException, ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.listLogFilePaths(execution, packageId);
	}

	@Override
	public List<String> getExecutionLogFilePaths(String buildCompositeKey, String executionId, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.listExecutionLogFilePaths(execution);
	}

	@Override
	public InputStream getExecutionLogFile(String buildCompositeKey, String executionId, String logFileName, User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecutionOrThrow(buildCompositeKey, executionId, authenticatedUser);
		return dao.getExecutionLogFileStream(execution, logFileName);
	}

	private Execution getExecutionOrThrow(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws ResourceNotFoundException {
		Execution execution = getExecution(buildCompositeKey, executionId, authenticatedUser);
		if (execution == null) {
			String item = CompositeKeyHelper.getPath(buildCompositeKey, executionId);
			throw new ResourceNotFoundException("Unable to find execution: " + item);
		}
		return execution;
	}

	private Execution getExecution(final String buildCompositeKey, final String executionId, final User authenticatedUser) throws ResourceNotFoundException {
		Build build = getBuild(buildCompositeKey, authenticatedUser);
		if (build == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return dao.find(build, executionId);
	}

	private Execution getExecution(final Build build, final Date creationTime) {
		return dao.find(build, EntityHelper.formatAsIsoDateTime(creationTime));
	}
	

	private Build getBuild(final String buildCompositeKey, final User authenticatedUser) throws ResourceNotFoundException {
		Long buildId = CompositeKeyHelper.getId(buildCompositeKey);
		if (buildId == null) {
			throw new ResourceNotFoundException("Unable to find build: " + buildCompositeKey);
		}
		return buildDAO.find(buildId, authenticatedUser);
	}

	private void generateReadmeFile(final Execution execution, final Package pkg) throws BadConfigurationException, JAXBException, IOException,
			ExecutionException, InterruptedException {
	    	LOGGER.info("Generating readMe file for package {}", pkg.getName());
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

	public void setFileProcessingFailureMaxRetry(final Integer fileProcessingFailureMaxRetry) {
		this.fileProcessingFailureMaxRetry = fileProcessingFailureMaxRetry;
	}

}
