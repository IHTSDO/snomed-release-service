package org.ihtsdo.buildcloud.service.classifier;

import static org.ihtsdo.buildcloud.service.build.RF2Constants.BETA_RELEASE_PREFIX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.ProcessingException;
import org.ihtsdo.snomed.util.rf2.schema.TableSchema;
import org.springframework.util.StreamUtils;

public abstract class RF2Classifier {
	
	public abstract File run(Build build, File equivalencyReportOutputFile,
			ClassificationInputInfo classificationInput, File resultDir) throws BusinessServiceException;
	
	public abstract ClassificationInputInfo constructClassificationInputInfo(Map<String, TableSchema> inputFileSchemaMap);
	
	public List<String> downloadFiles(final Build build, final File tempDir, final List<String> filenameList, BuildDAO buildDAO) throws ProcessingException {
		final List<String> localFilePaths = new ArrayList<>();
		boolean isBeta = build.getConfiguration().isBetaRelease();
		for (String downloadFilename : filenameList) {
			if (isBeta && !downloadFilename.startsWith(BETA_RELEASE_PREFIX)) {
				downloadFilename = BETA_RELEASE_PREFIX + downloadFilename;
			}
			final File localFile = new File(tempDir, downloadFilename);
			try (InputStream inputFileStream = buildDAO.getOutputFileInputStream(build, downloadFilename);
				 FileOutputStream out = new FileOutputStream(localFile)) {
				if (inputFileStream != null) {
					StreamUtils.copy(inputFileStream, out);
					localFilePaths.add(localFile.getAbsolutePath());
				} else {
					throw new ProcessingException("Didn't find output file:" + downloadFilename);
				}
			} catch (final IOException e) {
				throw new ProcessingException("Failed to download files for classification.", e);
			}
		}
		return localFilePaths;
	}	

	public static class ClassificationInputInfo {
		//snapshot files for cycle check and internal classifier but delta for external classifier
		private final List<String> conceptFilenames;
		private final List<String> statedRelationshipFilenames;
		//only required for external classifier
		private String owlAxiomRefsetDeltaFilename;
		private String mrcmAttributeDomainDeltaFilename;
		private List<String> localPreviousInferredRelationshipFilePaths;
		private List<String> localConceptFilePaths;
		private List<String> localStatedRelationshipFilePaths;
		boolean isExternal;
		

		ClassificationInputInfo(boolean isExternal) {
			conceptFilenames = new ArrayList<>();
			statedRelationshipFilenames = new ArrayList<>();
			localConceptFilePaths = new ArrayList<>();
			localPreviousInferredRelationshipFilePaths = new ArrayList<>();
			localStatedRelationshipFilePaths = new ArrayList<>();
			this.isExternal = isExternal;
		}
		
		public boolean isExternalClassifierUsed() {
			return this.isExternal;
		}

		public List<String> getLocalPreviousInferredRelationshipFilePaths() {
			return this.localPreviousInferredRelationshipFilePaths;
		}

		public void setLocalPreviousInferredRelationshipFilePaths(List<String> previousInferredRelationshipFilePaths) {
			this.localPreviousInferredRelationshipFilePaths = previousInferredRelationshipFilePaths;
		}

		public List<String> getLocalConceptFilePaths() {
			return this.localConceptFilePaths;
		}

		public List<String> getLocalStatedRelationshipFilePaths() {
			return this.localStatedRelationshipFilePaths;
		}

		public void setLocalStatedRelationshipFilePaths(List<String> localStatedRelationshipFilePaths) {
			this.localStatedRelationshipFilePaths = localStatedRelationshipFilePaths;
		}

		public void setLocalConceptFilePaths(List<String> localConceptFilePaths) {
			this.localConceptFilePaths = localConceptFilePaths;
		}

		public boolean isSufficientToClassify() {
			return !conceptFilenames.isEmpty() && !statedRelationshipFilenames.isEmpty();
		}

		public List<String> getConceptFilenames() {
			return conceptFilenames;
		}

		public List<String> getStatedRelationshipFilenames() {
			return statedRelationshipFilenames;
		}

		public String getOwlAxiomRefsetDeltaFilename() {
			return owlAxiomRefsetDeltaFilename;
		}

		public void setOwlAxiomRefsetDeltaFilename(String owlAxiomRefsetDeltaFilename) {
			this.owlAxiomRefsetDeltaFilename = owlAxiomRefsetDeltaFilename;
		}

		public String getMrcmAttributeDomainDeltaFilename() {
			return mrcmAttributeDomainDeltaFilename;
		}

		public void setMrcmAttributeDomainDeltaFilename(String mrcmAttributeDomainDeltaFilename) {
			this.mrcmAttributeDomainDeltaFilename = mrcmAttributeDomainDeltaFilename;
		}
	}
}
