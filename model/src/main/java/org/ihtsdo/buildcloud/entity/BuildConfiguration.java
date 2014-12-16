package org.ihtsdo.buildcloud.entity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.annotations.Type;

@Embeddable
public class BuildConfiguration {
	public static final String BETA_PREFIX = "x";

	private Date effectiveTime;
	@Column(columnDefinition = "TEXT")
	private String readmeHeader;
	private String readmeEndDate;
	@Type(type="yes_no")
	private boolean firstTimeRelease = false;
	@Type(type="yes_no")
	private boolean betaRelease = false;
	private String previousPublishedPackage;
	private String newRF2InputFiles;
	@Type(type="yes_no")
	private boolean justPackage = false;
	@Type(type="yes_no")
	private boolean workbenchDataFixesRequired = false;
	@Type(type="yes_no")
	private boolean createInferredRelationships = false;
	@Type(type="yes_no")
	private boolean createLegacyIds = false;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<RefsetCompositeKey> refsetCompositeKeys;

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
			Collections.addAll(files, newRF2InputFiles.split("\\|"));
		}
		return files;
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

	public boolean isCreateInferredRelationships() {
		return createInferredRelationships;
	}

	public void setCreateInferredRelationships(final boolean createInferredRelationships) {
		this.createInferredRelationships = createInferredRelationships;
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

	@Embeddable
	public static class RefsetCompositeKey {

		private String refsetId;

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

			if (!refsetId.equals(that.refsetId)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return refsetId.hashCode();
		}
	}

}