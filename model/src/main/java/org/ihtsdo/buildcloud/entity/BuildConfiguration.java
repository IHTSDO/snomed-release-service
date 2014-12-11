package org.ihtsdo.buildcloud.entity;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.text.ParseException;
import java.util.*;
import javax.persistence.*;

@Embeddable
public class BuildConfiguration {
	public static final String BETA_PREFIX = "x";

	private Date effectiveTime;
	@Column(columnDefinition = "TEXT")
	private String readmeHeader;
	private String readmeEndDate;
	private boolean firstTimeRelease = false;
	private boolean betaRelease = false;
	private String previousPublishedPackage;
	private String newRF2InputFiles;

	private boolean justPackage = false;
	private boolean workbenchDataFixesRequired = false;
	private boolean createInferredRelationships = false;
	private boolean createLegacyIds = false;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<RefsetCompositeKey> refsetCompositeKeys;

	public BuildConfiguration() {
	}

	@JsonIgnore
	public Date getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(Date effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@JsonProperty("effectiveTime")
	public String getEffectiveTimeFormatted() {
		return effectiveTime != null ? DateFormatUtils.ISO_DATE_FORMAT.format(effectiveTime) : null;
	}

	public void setEffectiveTimeFormatted(String effectiveTimeFormatted) throws ParseException {
		effectiveTime = DateFormatUtils.ISO_DATE_FORMAT.parse(effectiveTimeFormatted);
	}

	@JsonIgnore
	public String getEffectiveTimeSnomedFormat() {
		return effectiveTime != null ? DateFormatUtils.format(effectiveTime, Product.SNOMED_DATE_FORMAT) : null;
	}

	public Map<String, List<Integer>> getCustomRefsetCompositeKeys() {
		Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();
		if (refsetCompositeKeys != null) {
			for (RefsetCompositeKey refsetCompositeKey : refsetCompositeKeys) {
				String[] split = refsetCompositeKey.getFieldIndexes().split(",");
				List<Integer> indexes = new ArrayList<Integer>();
				for (String s : split) {
					indexes.add(Integer.parseInt(s.trim()));
				}
				map.put(refsetCompositeKey.getRefsetId(), indexes);
			}
		}
		return map;
	}

	public void setCustomRefsetCompositeKeys(Map<String, List<Integer>> customRefsetCompositeKeys) {
		Set<RefsetCompositeKey> keys = new HashSet<>();
		for (String key : customRefsetCompositeKeys.keySet()) {
			keys.add(new RefsetCompositeKey(key, customRefsetCompositeKeys.get(key).toString().replaceAll("[\\[\\]]", "")));
		}
		this.refsetCompositeKeys = keys;
	}

	@JsonIgnore
	public Set<RefsetCompositeKey> getRefsetCompositeKeys() {
		return refsetCompositeKeys;
	}

	public void setRefsetCompositeKeys(Set<RefsetCompositeKey> refsetCompositeKeys) {
		this.refsetCompositeKeys = refsetCompositeKeys;
	}

	@JsonIgnore
	public Set<String> getNewRF2InputFileSet() {
		Set<String> files = new HashSet<String>();
		if (newRF2InputFiles != null) {
			Collections.addAll(files, newRF2InputFiles.split("\\|"));
		}
		return files;
	}

	public String getReadmeHeader() {
		return readmeHeader;
	}

	public void setReadmeHeader(String readmeHeader) {
		this.readmeHeader = readmeHeader;
	}

	public String getReadmeEndDate() {
		return readmeEndDate;
	}

	public void setReadmeEndDate(String readmeEndDate) {
		this.readmeEndDate = readmeEndDate;
	}

	public boolean isFirstTimeRelease() {
		return firstTimeRelease;
	}

	public void setFirstTimeRelease(boolean firstTimeRelease) {
		this.firstTimeRelease = firstTimeRelease;
	}

	public boolean isBetaRelease() {
		return betaRelease;
	}

	public void setBetaRelease(boolean betaRelease) {
		this.betaRelease = betaRelease;
	}

	public boolean isWorkbenchDataFixesRequired() {
		return workbenchDataFixesRequired;
	}

	public void setWorkbenchDataFixesRequired(boolean workbenchDataFixesRequired) {
		this.workbenchDataFixesRequired = workbenchDataFixesRequired;
	}

	public boolean isJustPackage() {
		return justPackage;
	}

	public void setJustPackage(boolean justPackage) {
		this.justPackage = justPackage;
	}

	public String getPreviousPublishedPackage() {
		return previousPublishedPackage;
	}

	public void setPreviousPublishedPackage(String previousPublishedPackage) {
		this.previousPublishedPackage = previousPublishedPackage;
	}

	public boolean isCreateInferredRelationships() {
		return createInferredRelationships;
	}

	public void setCreateInferredRelationships(boolean createInferredRelationships) {
		this.createInferredRelationships = createInferredRelationships;
	}

	public boolean isCreateLegacyIds() {
		return createLegacyIds;
	}

	public void setCreateLegacyIds(boolean createLegacyIds) {
		this.createLegacyIds = createLegacyIds;
	}

	public String getNewRF2InputFiles() {
		return newRF2InputFiles;
	}

	public void setNewRF2InputFiles(String newRF2InputFiles) {
		this.newRF2InputFiles = newRF2InputFiles;
	}

	@Embeddable
	public static class RefsetCompositeKey {

		private String refsetId;

		private String fieldIndexes;

		private RefsetCompositeKey() {
		}

		public RefsetCompositeKey(String refsetId, String fieldIndexes) {
			this.refsetId = refsetId;
			this.fieldIndexes = fieldIndexes;
		}

		public String getRefsetId() {
			return refsetId;
		}

		public void setRefsetId(String refsetId) {
			this.refsetId = refsetId;
		}

		public String getFieldIndexes() {
			return fieldIndexes;
		}

		public void setFieldIndexes(String fieldIndexes) {
			this.fieldIndexes = fieldIndexes;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			RefsetCompositeKey that = (RefsetCompositeKey) o;

			if (!refsetId.equals(that.refsetId)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return refsetId.hashCode();
		}
	}

}