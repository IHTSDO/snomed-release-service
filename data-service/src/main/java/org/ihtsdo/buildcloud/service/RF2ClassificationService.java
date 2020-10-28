package org.ihtsdo.buildcloud.service;

import com.google.common.io.Files;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.service.classifier.ExternalRF2ClassifierRestClient;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.otf.utils.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.*;

@Service
public class RF2ClassificationService {

	@Autowired
	private ExternalRF2ClassifierRestClient classifierRestClient;

	@Autowired
	private BuildDAO buildDAO;

	@Autowired
	private ProductInputFileService productInputFileService;

	private static final String OWL_REFSET_FILE_PATTERN = ".*_sRefset_OWL.*";

	private static final String REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA = "rel2_cissccRefset_MRCMAttributeDomainDelta";

	private static final String REL2_CONCEPT_PATTERN = ".*rel2_Concept_.*Delta.*";

	// sct2_RelationshipConcreteValues_Delta or sct2_Relationship_Delta
	private static final String REL2_RELATIONSHIP_PATTERN = ".*rel2_Relationship.*Delta.*";

	private static final String REL2_STATED_RELATIONSHIP_PATTERN = ".*rel2_StatedRelationship_.*Delta.*";

	private static final String REFSET = "Refset_";

	public static final String EQUIVALENT_CONCEPT_REFSET = "der2_sRefset_EquivalentConceptSimpleMapDelta";

	private static final Logger LOGGER = LoggerFactory.getLogger(RF2ClassificationService.class);

	public File classify(Build build) throws BusinessServiceException {
		LOGGER.info("Run classification for build {}", build.getProduct() + build.getId());
		String previousPublished = build.getConfiguration().getPreviousPublishedPackage();
		String dependencyRelease = null;
		ExtensionConfig extensionConfig = build.getConfiguration().getExtensionConfig();
		if (extensionConfig != null) {
			dependencyRelease = extensionConfig.getDependencyRelease();
			if (dependencyRelease == null || dependencyRelease.isEmpty()) {
				if (extensionConfig.isReleaseAsAnEdition()) {
					LOGGER.warn("The product is configured as an edition without dependency package. Only previous package {} will be used in classification", previousPublished);
				} else {
					throw new BusinessServiceException("International dependency release can't be null for extension release build.");
				}
			}
		}
		File deltaTempDir = Files.createTempDir();
		File zipTempDir = Files.createTempDir();
		File rf2DeltaZipFile = new File(zipTempDir, "rf2Delta_" + build.getId() + ".zip");
		// Create a Delta package based on the input files and send to classification service
		try {
			createDeltaArchiveForClassification(build, deltaTempDir);
			ZipFileUtils.zip(deltaTempDir.getAbsolutePath(), rf2DeltaZipFile.getAbsolutePath());
		} catch (ProcessingException | IOException e) {
			throw new BusinessServiceException(e);
		}
		// Classify
		return classifierRestClient.classify(rf2DeltaZipFile, previousPublished, dependencyRelease);
	}

		private List<String> createDeltaArchiveForClassification(final Build build, final File deltaTempDir) throws ProcessingException, IOException {
			LOGGER.info("Creating delta archive for classification");
			final List<String> localFilePaths = new ArrayList<>();
			for (String downloadFilename : buildDAO.listInputFileNames(build)) {
				if (!isRequiredFileForClassification(downloadFilename)) continue;
				LOGGER.info("Prepare {} for archiving", downloadFilename);
				String rename = (downloadFilename.contains(REFSET) && !downloadFilename.matches(OWL_REFSET_FILE_PATTERN)) ? downloadFilename.replace(INPUT_FILE_PREFIX, DER2) : downloadFilename.replace(INPUT_FILE_PREFIX, SCT2);
				LOGGER.info("Rename {} to {} for archiving", downloadFilename, rename);
				final File localFile = new File(deltaTempDir, rename);
				try (InputStream inputFileStream = productInputFileService.getFileInputStream(build.getProduct().getReleaseCenter().getBusinessKey()
						, build.getProduct().getBusinessKey(), downloadFilename);
				FileOutputStream out = new FileOutputStream(localFile)) {
					if (inputFileStream != null) {
						StreamUtils.copy(inputFileStream, out);
						localFilePaths.add(localFile.getAbsolutePath());
					} else {
						throw new ProcessingException("Didn't find input file:" + downloadFilename);
					}
				}

			}
			return localFilePaths;
		}

		private boolean isRequiredFileForClassification(String filename) {
			return filename.matches(REL2_CONCEPT_PATTERN) || filename.matches(REL2_STATED_RELATIONSHIP_PATTERN) || filename.matches(REL2_RELATIONSHIP_PATTERN)
					|| filename.contains(REL2_MRCM_ATTRIBUTE_DOMAIN_DELTA) || filename.matches(OWL_REFSET_FILE_PATTERN);
		}


	}
