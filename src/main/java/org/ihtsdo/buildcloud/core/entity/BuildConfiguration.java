package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.util.*;

@Entity
@Table(name="build_config")
public class BuildConfiguration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private long id;
	public static final String BETA_PREFIX = "x";

	@OneToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;
	
	@Column(name="effective_time")
	private Date effectiveTime;
	
	@Column(name="readme_header",columnDefinition = "TEXT")
	private String readmeHeader;
	
	@Column(name="readme_end_date")
	private String readmeEndDate;
	
	@Type(type="yes_no")
	@Column(name="first_time_release")
	private boolean firstTimeRelease = false;
	
	@Type(type="yes_no")
	@Column(name="beta_release")
	private boolean betaRelease = false;
	
	@Column(name="previous_published_release")
	private String previousPublishedPackage;
	@Column(name="rf2_input_files")
	private String newRF2InputFiles;
	
	@Type(type="yes_no")
	@Column(name="just_package")
	private boolean justPackage = false;
	
	@Type(type="yes_no")
	@Column(name="require_wb_data_fix")
	private boolean workbenchDataFixesRequired = false;
	
	@Type(type="yes_no")
	@Column(name="require_input_files_fix")
	private boolean inputFilesFixesRequired = false;
	
	@Type(type="yes_no")
	@Column(name="create_legacy_ids")
	private boolean createLegacyIds = false;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name="refset_composite_key", joinColumns=@JoinColumn(name="build_config_id"))
	private Set<RefsetCompositeKey> refsetCompositeKeys;
	
	@OneToOne (mappedBy="buildConfiguration", cascade=CascadeType.ALL)
	private ExtensionConfig extensionConfig;
	
	@Column(name = "include_prev_release_files")
	private String includePrevReleaseFiles;
	
	@Type(type="yes_no")
	@Column(name = "daily_build")
	private boolean dailyBuild;

	@Type(type="yes_no")
	@Column(name = "classify_output_files")
	private boolean classifyOutputFiles;

	@Column(name="licence_statement")
	private String licenceStatement;

	@Column(name="release_information_fields")
	private String releaseInformationFields;

	@Type(type="yes_no")
	@Column(name = "use_classifier_precondition_checks")
	private boolean useClassifierPreConditionChecks;

	@Column(name="concept_preferred_terms")
	private String conceptPreferredTerms;

	@Column(name="default_branch_path")
	private String defaultBranchPath;

	@Transient
	private String branchPath;

	@Transient
	private String buildName;

	@Transient
	private Set<String> excludedModuleIds;

	@Transient
	private boolean loadExternalRefsetData;

	@Transient
	private boolean loadTermServerData;

	@Transient
	private String exportType;

	public BuildConfiguration() {
	}

	@JsonIgnore
	public Date getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(final Date effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@JsonProperty("effectiveTime")
	public String getEffectiveTimeFormatted() {
		return effectiveTime != null ? DateFormatUtils.ISO_DATE_FORMAT.format(effectiveTime) : null;
	}

	public void setEffectiveTimeFormatted(final String effectiveTimeFormatted) throws ParseException {
		effectiveTime = DateFormatUtils.ISO_DATE_FORMAT.parse(effectiveTimeFormatted);
	}

	@JsonIgnore
	public String getEffectiveTimeSnomedFormat() {
		return effectiveTime != null ? DateFormatUtils.format(effectiveTime, Product.SNOMED_DATE_FORMAT) : null;
	}

	public Map<String, List<Integer>> getCustomRefsetCompositeKeys() {
		final Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();
		if (refsetCompositeKeys != null) {
			for (final RefsetCompositeKey refsetCompositeKey : refsetCompositeKeys) {
				final String[] split = refsetCompositeKey.getFieldIndexes().split(",");
				final List<Integer> indexes = new ArrayList<Integer>();
				for (final String s : split) {
					indexes.add(Integer.parseInt(s.trim()));
				}
				map.put(refsetCompositeKey.getRefsetId(), indexes);
			}
		}
		return map;
	}

	public void setCustomRefsetCompositeKeys(final Map<String, List<Integer>> customRefsetCompositeKeys) {
		final Set<RefsetCompositeKey> keys = new HashSet<>();
		for (final String key : customRefsetCompositeKeys.keySet()) {
			keys.add(new RefsetCompositeKey(key, customRefsetCompositeKeys.get(key).toString().replaceAll("[\\[\\]]", "")));
		}
		this.refsetCompositeKeys = keys;
	}

	@JsonIgnore
	public Set<RefsetCompositeKey> getRefsetCompositeKeys() {
		return refsetCompositeKeys;
	}

	public void setRefsetCompositeKeys(final Set<RefsetCompositeKey> refsetCompositeKeys) {
		this.refsetCompositeKeys = refsetCompositeKeys;
	}

	@JsonIgnore
	public Set<String> getNewRF2InputFileSet() {
		final Set<String> files = new HashSet<String>();
		if (newRF2InputFiles != null) {
			for (String newInputile :newRF2InputFiles.split("\\|")) {
				if (!Normalizer.isNormalized(newInputile, Form.NFC)) {
					newInputile = Normalizer.normalize(newInputile, Form.NFC);
				}

				if (newInputile.startsWith("x")) {
					newInputile = newInputile.replaceFirst("x", "");
				}
				newInputile = newInputile.replace("der2", "rel2").replace("sct2", "rel2");

				files.add(newInputile);
			}
		}
		return files;
	}

	@JsonIgnore
	public Map<String, Set<String>> getIncludedFilesInNewFilesMap() {
		final Map<String, Set<String>> fileMaps = new HashMap<>();
		if(StringUtils.isNotBlank(includePrevReleaseFiles)) {
			String[] configurations = includePrevReleaseFiles.split("\\|");
			for (String configuration : configurations) {
				String key = configuration.substring(0, configuration.indexOf("("));
				String[] mapFiles = configuration.substring(configuration.indexOf("(")+1,configuration.indexOf(")")).split(",");
				Set<String> filesList = new HashSet<>();
				for (String mapFile : mapFiles) {
					if (!Normalizer.isNormalized(mapFile, Form.NFC)) {
						mapFile = Normalizer.normalize(mapFile, Form.NFC);
					}
					filesList.add(mapFile.trim());
				}
				fileMaps.put(key.trim(), filesList);
			}
		}
		return fileMaps;
	}

	public String getReadmeHeader() {
		return readmeHeader;
	}

	public void setReadmeHeader(final String readmeHeader) {
		this.readmeHeader = readmeHeader;
	}

	public String getReadmeEndDate() {
		return readmeEndDate;
	}

	public void setReadmeEndDate(final String readmeEndDate) {
		this.readmeEndDate = readmeEndDate;
	}

	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}

	public void setFirstTimeRelease(final boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
	}

	public boolean isBetaRelease() {
		return betaRelease;
	}

	public void setBetaRelease(final boolean betaRelease) {
		this.betaRelease = betaRelease;
	}

	public boolean isWorkbenchDataFixesRequired() {
		return workbenchDataFixesRequired;
	}

	public void setWorkbenchDataFixesRequired(final boolean workbenchDataFixesRequired) {
		this.workbenchDataFixesRequired = workbenchDataFixesRequired;
	}

	public boolean isJustPackage() {
		return justPackage;
	}

	public void setJustPackage(final boolean justPackage) {
		this.justPackage = justPackage;
	}

	public String getPreviousPublishedPackage() {
		return previousPublishedPackage;
	}

	public void setPreviousPublishedPackage(final String previousPublishedPackage) {
		this.previousPublishedPackage = previousPublishedPackage;
	}

	public boolean isCreateLegacyIds() {
		return createLegacyIds;
	}

	public void setCreateLegacyIds(final boolean createLegacyIds) {
		this.createLegacyIds = createLegacyIds;
	}

	public String getNewRF2InputFiles() {
		return newRF2InputFiles;
	}

	public void setNewRF2InputFiles(final String newRF2InputFiles) {
		this.newRF2InputFiles = newRF2InputFiles;
	}
	
	public Product getProduct() {
		return product;
	}

	public void setProduct(final Product product) {
		this.product = product;
	}
	
	public ExtensionConfig getExtensionConfig() {
		return this.extensionConfig;
	}

	public void setExtensionConfig(ExtensionConfig extensionConfig) {
		this.extensionConfig = extensionConfig;
	}

	public boolean isClassifyOutputFiles() {
		return classifyOutputFiles;
	}

	public void setClassifyOutputFiles(boolean classifyOutputFiles) {
		this.classifyOutputFiles = classifyOutputFiles;
	}

	public String getLicenceStatement() {
		return licenceStatement;
	}

	public void setLicenceStatement(String licenceStatement) {
		this.licenceStatement = licenceStatement;
	}

	public String getReleaseInformationFields() {
		return releaseInformationFields;
	}

	public void setReleaseInformationFields(String releaseInformationFields) {
		this.releaseInformationFields = releaseInformationFields;
	}

	public boolean useClassifierPreConditionChecks() {
		return useClassifierPreConditionChecks;
	}

	public boolean isUseClassifierPreConditionChecks() {
		return useClassifierPreConditionChecks;
	}

	public void setUseClassifierPreConditionChecks(boolean useClassifierPreConditionChecks) {
		this.useClassifierPreConditionChecks = useClassifierPreConditionChecks;
	}

	public String getConceptPreferredTerms() {
		return conceptPreferredTerms;
	}

	public void setConceptPreferredTerms(String conceptPreferredTerms) {
		this.conceptPreferredTerms = conceptPreferredTerms;
	}

	public String getDefaultBranchPath() {
		return defaultBranchPath;
	}

	public void setDefaultBranchPath(String defaultBranchPath) {
		this.defaultBranchPath = defaultBranchPath;
	}

	public String getBranchPath() {
		return branchPath;
	}

	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	public String getBuildName() {
		return buildName;
	}

	public void setBuildName(String buildName) {
		this.buildName = buildName;
	}

	public String getExportType() {
		return exportType;
	}

	public void setExportType(String exportType) {
		this.exportType = exportType;
	}

	@Override
	public String toString() {
		return "BuildConfiguration{" +
				"id=" + id +
				", effectiveTime=" + effectiveTime +
				", readmeEndDate='" + readmeEndDate + '\'' +
				", firstTimeRelease=" + firstTimeRelease +
				", betaRelease=" + betaRelease +
				", previousPublishedPackage='" + previousPublishedPackage + '\'' +
				", newRF2InputFiles='" + newRF2InputFiles + '\'' +
				", justPackage=" + justPackage +
				", workbenchDataFixesRequired=" + workbenchDataFixesRequired +
				", inputFilesFixesRequired=" + inputFilesFixesRequired +
				", createLegacyIds=" + createLegacyIds +
				", refsetCompositeKeys=" + refsetCompositeKeys +
				", extensionConfig=" + extensionConfig +
				", includePrevReleaseFiles='" + includePrevReleaseFiles + '\'' +
				", dailyBuild=" + dailyBuild +
				", classifyOutputFiles=" + classifyOutputFiles +
				", licenceStatement='" + licenceStatement + '\'' +
				", releaseInformationFields='" + releaseInformationFields + '\'' +
				", useClassifierPreConditionChecks=" + useClassifierPreConditionChecks +
				", conceptPreferredTerms='" + conceptPreferredTerms + '\'' +
				", defaultBranchPath='" + defaultBranchPath + '\'' +
				", branchPath='" + branchPath + '\'' +
				", buildName='" + buildName + '\'' +
				", excludedModuleIds=" + excludedModuleIds +
				", loadExternalRefsetData=" + loadExternalRefsetData +
				", loadTermServerData=" + loadTermServerData +
				", exportType='" + exportType + '\'' +
				'}';
	}

	@Embeddable
	public static class RefsetCompositeKey {
		@Column(name="refset_id")
		private String refsetId;
		@Column(name="field_indexes")
		private String fieldIndexes;

		private RefsetCompositeKey() {
		}

		public RefsetCompositeKey(final String refsetId, final String fieldIndexes) {
			this.refsetId = refsetId;
			this.fieldIndexes = fieldIndexes;
		}

		public String getRefsetId() {
			return refsetId;
		}

		public void setRefsetId(final String refsetId) {
			this.refsetId = refsetId;
		}

		public String getFieldIndexes() {
			return fieldIndexes;
		}

		public void setFieldIndexes(final String fieldIndexes) {
			this.fieldIndexes = fieldIndexes;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final RefsetCompositeKey that = (RefsetCompositeKey) o;

			return refsetId.equals(that.refsetId);
		}

		@Override
		public int hashCode() {
			return refsetId.hashCode();
		}
	}

	public boolean isInputFilesFixesRequired() {
		return inputFilesFixesRequired;
	}

	public void setInputFilesFixesRequired(boolean inputFilesFixesRequired) {
		this.inputFilesFixesRequired = inputFilesFixesRequired;
	}

	public String getIncludePrevReleaseFiles() {
		return includePrevReleaseFiles;
	}

	public void setIncludePrevReleaseFiles(String includePrevReleaseFiles) {
		this.includePrevReleaseFiles = includePrevReleaseFiles;
	}

	public boolean isDailyBuild() {
		return dailyBuild;
	}

	public void setExcludedModuleIds(Set<String> excludedModuleIds) {
		this.excludedModuleIds = excludedModuleIds;
	}

	public Set<String> getExcludedModuleIds() {
		return excludedModuleIds;
	}

	public void setLoadExternalRefsetData(boolean loadExternalRefsetData) {
		this.loadExternalRefsetData = loadExternalRefsetData;
	}

	public boolean isLoadExternalRefsetData() {
		return loadExternalRefsetData;
	}

	public void setLoadTermServerData(boolean loadTermServerData) {
		this.loadTermServerData = loadTermServerData;
	}

	public boolean isLoadTermServerData() {
		return loadTermServerData;
	}
}