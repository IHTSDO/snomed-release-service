package org.ihtsdo.buildcloud.core.service;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.ihtsdo.buildcloud.core.dao.BuildDAO;
import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.Build.Status;
import org.ihtsdo.buildcloud.core.entity.BuildConfiguration;
import org.ihtsdo.buildcloud.core.entity.Product;
import org.ihtsdo.buildcloud.core.manifest.FileType;
import org.ihtsdo.buildcloud.core.manifest.FolderType;
import org.ihtsdo.buildcloud.core.manifest.ListingType;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.buildcloud.core.service.inputfile.gather.InputGatherReport;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.FileProcessingReportDetail;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.ReportType;
import org.ihtsdo.buildcloud.core.service.inputfile.prepare.SourceFileProcessingReport;
import org.ihtsdo.buildcloud.telemetry.client.TelemetryStream;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.BusinessServiceRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReleaseServiceImpl implements ReleaseService {

	private static final String TRACKER_ID = "trackerId";

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private InputFileService inputFileService;

	@Autowired
	private ProductService productService;

	@Autowired
	private BuildService buildService;

	@Autowired
	private ExternalMaintainedRefsetsService externalMaintainedRefsetsService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseServiceImpl.class);

	private static final String ERROR_MSG_FORMAT = "Error encountered while running release build %s for product %s";

	@Override
	public void runReleaseBuild(Build build, Authentication authentication) {
		TelemetryStream.start(LOGGER, buildDAO.getTelemetryBuildLogFilePath(build));
		Product product = build.getProduct();

		try {
			MDC.put(TRACKER_ID, product.getReleaseCenter().getBusinessKey() + "|" + product.getBusinessKey() + "|" + build.getId());
			LOGGER.info("Running release build for center/{}/product/{}/buildId/{}", product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build.getId());

			if (gatherSourceFiles(build, product, authentication) && prepareInputFiles(build, product)) {
				// trigger build
				LOGGER.info("Build {} is triggered for product {}", build.getId(), build.getProduct().getBusinessKey());
				buildService.triggerBuild(build, false);
			}
		} catch (Exception e) {
			String msg = String.format(ERROR_MSG_FORMAT, build.getId(), build.getProduct().getBusinessKey());
			LOGGER.error(msg, e);
			buildDAO.updateStatus(build, Status.FAILED);
		} finally {
			MDC.remove(TRACKER_ID);
			TelemetryStream.finish(LOGGER);
		}
	}

	@Override
	public void startNewAuthoringCycle(String releaseCenterKey, String productKey, String effectiveTime, String productKeySource, String dependencyPackage) throws BusinessServiceException, ParseException, JAXBException, IOException {
		Product product = productService.find(releaseCenterKey, productKey, false);
		if (product == null) {
			throw new BusinessServiceRuntimeException("Could not find product " + productKey + " in release center " + releaseCenterKey);
		}
		Product productSource = productService.find(releaseCenterKey, productKeySource, false);
		if (productSource == null) {
			throw new BusinessServiceRuntimeException("Could not find product " + productKeySource + " in release center " + releaseCenterKey);
		}

		List<Build> builds = buildService.findAllDesc(releaseCenterKey, productKeySource, false, false, false, null);
		Build latestPublishedBuild = null;
		for (int i = 0; i < builds.size(); i++) {
			if (builds.get(i).getTags() != null && builds.get(i).getTags().contains(Build.Tag.PUBLISHED)) {
				latestPublishedBuild = builds.get(i);
				break;
			}
		}
		if (latestPublishedBuild != null) {
			latestPublishedBuild = buildService.find(releaseCenterKey, productKeySource, latestPublishedBuild.getId(), true, false, false, null);
			BuildConfiguration configuration = latestPublishedBuild.getConfiguration();
			if (configuration.getEffectiveTime().compareTo(DateFormatUtils.ISO_DATE_FORMAT.parse(effectiveTime)) >= 0) {
				throw new BusinessServiceRuntimeException("The new effective time must be greater than the latest published effective time " + configuration.getEffectiveTimeFormatted());
			}

			String previousPackage = getPreviousPackage(latestPublishedBuild) + RF2Constants.ZIP_FILE_EXTENSION;
			replaceManifestFile(releaseCenterKey, productKey, latestPublishedBuild, effectiveTime, configuration.getEffectiveTimeSnomedFormat());
			externalMaintainedRefsetsService.copyExternallyMaintainedFiles(releaseCenterKey, configuration.getEffectiveTimeSnomedFormat(), effectiveTime.replaceAll("-", ""), true);
			updateProduct(releaseCenterKey, productKey, effectiveTime, dependencyPackage, previousPackage);
		} else {
			LOGGER.info("The product {} does not have any build which has been published yet", productKeySource);
		}
	}

	private void updateProduct(String releaseCenterKey, String productKey, String effectiveTime, String dependencyPackage, String previousPackage) throws BusinessServiceException {
		Map<String, String> changes = new HashMap<>();
		changes.put(ProductService.DAILY_BUILD, ProductService.TRUE);
		changes.put(ProductService.EFFECTIVE_TIME, effectiveTime);
		changes.put(ProductService.PREVIOUS_PUBLISHED_PACKAGE, previousPackage);
		if (!StringUtils.isEmpty(dependencyPackage)) {
			changes.put(ProductService.DEPENDENCY_RELEASE_PACKAGE, dependencyPackage);
			changes.put(ProductService.EXTENSION_DEPENDENCY_RELEASE, dependencyPackage); // QA config
		}
		if ("international".equalsIgnoreCase(releaseCenterKey)) {
			changes.put(ProductService.PREVIOUS_INTERNATIONAL_RELEASE, previousPackage); // QA config
		} else {
			changes.put(ProductService.PREVIOUS_EXTENSION_RELEASE, previousPackage); // QA config
		}
		productService.update(releaseCenterKey, productKey, changes);
	}

	private String getPreviousPackage(final Build build) throws JAXBException {
		final InputStream manifestStream = buildDAO.getManifestStream(build);
		final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
		final ListingType manifestListing = unmarshaller.unmarshal(new StreamSource(manifestStream), ListingType.class).getValue();
		String latestPackageName = null;
		if (manifestListing != null) {
			final FolderType rootFolder = manifestListing.getFolder();
			if (rootFolder != null) {
				latestPackageName = rootFolder.getName();
			}
		}
		if (latestPackageName == null) {
			throw new BusinessServiceRuntimeException("Could not find package name from manifest.xml");
		}
		return latestPackageName;
	}

	private void replaceManifestFile(String releaseCenterKey, String productKey, Build build, String effectiveTime, String previousEffectiveTime) throws IOException {
		File tmpFile = File.createTempFile("manifest", ".xml");
		try {
			final InputStream manifestStream = buildDAO.getManifestStream(build);
			final Unmarshaller unmarshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createUnmarshaller();
			ListingType manifestListing = unmarshaller.unmarshal(new StreamSource(manifestStream), ListingType.class).getValue();
			FolderType rootFolder = manifestListing.getFolder();
			boolean isDeltaFolderExistInManifest = isDeltaFolderExistInManifest(rootFolder);
			if (!isDeltaFolderExistInManifest) {
				FolderType fullFolder = null;
				FolderType snapshotFolder = null;
				String newFileName = replaceReleasePackageName(rootFolder.getName());
				newFileName = replaceEffectiveTime(newFileName, previousEffectiveTime, effectiveTime);
				rootFolder.setName(newFileName);
				for (FileType fileType : rootFolder.getFile()) {
					fileType.setName(replaceEffectiveTime(fileType.getName(), previousEffectiveTime, effectiveTime));
				}
				for (FolderType subFolder : rootFolder.getFolder()) {
					if (RF2Constants.FULL.equals(subFolder.getName())) {
						fullFolder = subFolder;
					}
					if (RF2Constants.SNAPSHOT.equals(subFolder.getName())) {
						snapshotFolder = subFolder;
					}
				}

				// Re-use SNAPSHOT as DELTA
				if (snapshotFolder != null) {
					snapshotFolder.setName(RF2Constants.DELTA);
					renameFileName(snapshotFolder, previousEffectiveTime, effectiveTime, RF2Constants.SNAPSHOT, RF2Constants.DELTA);
				}

				// Copy all Full files and rename to Snapshot files
				if (fullFolder != null) {
					// update effective time for Full files
					renameFileName(fullFolder, previousEffectiveTime, effectiveTime, RF2Constants.FULL, RF2Constants.FULL);

					Gson gson = new Gson();
					FolderType newSnapshotFolder = gson.fromJson(gson.toJson(fullFolder), FolderType.class);
					newSnapshotFolder.setName(RF2Constants.SNAPSHOT);
					renameFileName(newSnapshotFolder, previousEffectiveTime, effectiveTime, RF2Constants.FULL, RF2Constants.SNAPSHOT);
					rootFolder.getFolder().add(newSnapshotFolder);
				}
				manifestListing.setFolder(rootFolder);

				Marshaller marshaller = JAXBContext.newInstance(RF2Constants.MANIFEST_CONTEXT_PATH).createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				marshaller.marshal(manifestListing, tmpFile);
			} else {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(buildDAO.getManifestStream(build)));
					 PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tmpFile)));) {
					String str;
					while ((str = reader.readLine()) != null) {
						str = replaceReleasePackageName(str);
						str = replaceEffectiveTime(str, previousEffectiveTime, effectiveTime);
						writer.println(str);
					}
				} catch (FileNotFoundException e) {
					LOGGER.error(e.getMessage());
				}
			}
			inputFileService.putManifestFile(releaseCenterKey, productKey, new FileInputStream(tmpFile), "manifest.xml", tmpFile.length());
		} catch (JAXBException e) {
			LOGGER.error(e.getMessage());

		} finally {
			FileUtils.forceDelete(tmpFile);
		}
	}

	private boolean isDeltaFolderExistInManifest(FolderType rootFolder) {
		boolean isDeltaFolderExistInManifest = false;
		if (rootFolder.getFolder() != null) {
			for (FolderType subFolder : rootFolder.getFolder()) {
				if (RF2Constants.DELTA.equals(subFolder.getName())) {
					isDeltaFolderExistInManifest = true;
					break;
				}
			}
		}
		return isDeltaFolderExistInManifest;
	}

	private void renameFileName(FolderType folder, String previousEffectiveTime, String effectiveTime, String fromFile, String toFile) {
		if (folder.getFile() != null) {
			for (FileType fileType : folder.getFile()) {
				String newFileName = replaceEffectiveTime(fileType.getName(), previousEffectiveTime, effectiveTime);
				newFileName = newFileName.replaceAll(fromFile, toFile);
				fileType.setName(newFileName);
			}
		}
		if (folder.getFolder() != null) {
			for (FolderType folderType : folder.getFolder()) {
				renameFileName(folderType, previousEffectiveTime, effectiveTime, fromFile, toFile);
			}
		}
	}

	private String replaceEffectiveTime(String filename, String previousEffectiveTime, String effectiveTime) {
		return filename.replaceAll("_" + previousEffectiveTime, "_" + effectiveTime.replaceAll("-", ""));
	}

	private String replaceReleasePackageName(String filename) {
		return filename.replaceAll("PRODUCTION", "DAILYBUILD");
	}

	private boolean prepareInputFiles(Build build, Product product) throws BusinessServiceException {
		// Prepare input files from sources when available
		SourceFileProcessingReport sourceFileProcessingReport = inputFileService.prepareInputFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build, true);
		if (sourceFileProcessingReport.getDetails().get(ReportType.ERROR) != null) {
			LOGGER.error("Error occurred when processing input files");
			List<FileProcessingReportDetail> errorDetails = sourceFileProcessingReport.getDetails().get(ReportType.ERROR);
			for (FileProcessingReportDetail errorDetail : errorDetails) {
				LOGGER.error("File: {} -> Error Details: {}", errorDetail.getFileName(), errorDetail.getMessage());
			}
			buildDAO.updateStatus(build, Status.FAILED_INPUT_PREPARE_REPORT_VALIDATION);
			LOGGER.error("Errors found in the source file prepare report. Please see detailed failures via the inputPrepareReport_url link listed.");
			return false;
		}
		return true;
	}

	private boolean gatherSourceFiles(Build build, Product product, Authentication authentication) throws BusinessServiceException, IOException {
		SecurityContext securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(authentication);

		// Gather all files in term server and externally maintain buckets when required
		InputGatherReport inputGatherReport = inputFileService.gatherSourceFiles(product.getReleaseCenter().getBusinessKey(), product.getBusinessKey(), build, securityContext);
		if (inputGatherReport.getStatus().equals(InputGatherReport.Status.ERROR)) {
			LOGGER.error("Error occurred when gathering source files: ");
			for (String source : inputGatherReport.getDetails().keySet()) {
				InputGatherReport.Details details = inputGatherReport.getDetails().get(source);
				if (InputGatherReport.Status.ERROR.equals(details.getStatus())) {
					LOGGER.error("Source: {} -> Error Details: {}", source, details.getMessage());
				}
			}
			buildDAO.updateStatus(build, Status.FAILED_INPUT_GATHER_REPORT_VALIDATION);
			LOGGER.error("Failed when gathering source files. Please see detailed failures via the inputGatherReport_url link listed");
			return false;
		}
		return true;
	}
}
