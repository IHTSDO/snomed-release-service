package org.ihtsdo.buildcloud.entity;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.*;
import javax.persistence.*;

@Entity
@JsonPropertyOrder({"id", "name"})
public class Product {

	public static final String SNOMED_DATE_FORMAT = "yyyyMMdd";

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	private Date effectiveTime;

	@JsonIgnore
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private ReleaseCenter releaseCenter;

	@Transient
	@JsonIgnore
	private List<String> inputFiles;

	@Column(columnDefinition = "TEXT")
	private String readmeHeader;

	private String readmeEndDate;

	private boolean firstTimeRelease = false;

	private boolean workbenchDataFixesRequired = false;

	private boolean justPackage = false;

	private String previousPublishedPackage;
	private boolean createInferredRelationships;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<RefsetCompositeKey> refsetCompositeKeys;

	private String newRF2InputFiles;

	public Product() {

	}

	public Product(String name) {
		this();
		setName(name);
		inputFiles = new ArrayList<>();
	}
	
	public Product(Long id, String name) {
		this(name);
		this.id = id;
	}

	@JsonProperty("effectiveTime")
	public String getEffectiveDateFormatted() {
		return effectiveTime != null ? DateFormatUtils.ISO_DATE_FORMAT.format(effectiveTime) : null;
	}

	@JsonIgnore
	public String getEffectiveTimeSnomedFormat() {
		return effectiveTime != null ? DateFormatUtils.format(effectiveTime, SNOMED_DATE_FORMAT) : null;
	}

	public void setCustomRefsetCompositeKeys(Map<String, List<Integer>> customRefsetCompositeKeys) {
		this.refsetCompositeKeys = toRefsetCompositeKeys(customRefsetCompositeKeys);
	}

	private Set<RefsetCompositeKey> toRefsetCompositeKeys(Map<String, List<Integer>> customRefsetCompositeKeys) {
		Set<RefsetCompositeKey> keys = new HashSet<>();
		for (String key : customRefsetCompositeKeys.keySet()) {
			keys.add(new RefsetCompositeKey(key, customRefsetCompositeKeys.get(key).toString().replaceAll("[\\[\\]]", "")));
		}
		return keys;
	}

	@JsonIgnore
	public Map<String, List<Integer>> getCustomRefsetCompositeKeysMap() {
		Map<String, List<Integer>> map = new HashMap<>();
		if (refsetCompositeKeys != null) {
			for (RefsetCompositeKey refsetCompositeKey : refsetCompositeKeys) {
				String[] split = refsetCompositeKey.getFieldIndexes().split(",");
				List<Integer> indexes = new ArrayList<>();
				for (String s : split) {
					indexes.add(Integer.parseInt(s.trim()));
				}
				map.put(refsetCompositeKey.getRefsetId(), indexes);
			}
		}
		return map;
	}

	@JsonIgnore
	public Set<String> getNewRF2InputFileSet() {
		Set<String> files = new HashSet<>();
		if (newRF2InputFiles != null) {
			Collections.addAll(files, newRF2InputFiles.split("\\|"));
		}
		return files;
	}

	public void setName(String name) {
		this.name = name;
		generateBusinessKey();
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public Date getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(Date effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@JsonProperty("id")
	public String getBusinessKey() {
		return businessKey;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public void setBusinessKey(String businessKey) {
		this.businessKey = businessKey;
	}

	public List<String> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<String> inputFiles) {
		this.inputFiles = inputFiles;
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

	public Set<RefsetCompositeKey> getRefsetCompositeKeys() {
		return refsetCompositeKeys;
	}

	public void setRefsetCompositeKeys(Set<RefsetCompositeKey> refsetCompositeKeys) {
		this.refsetCompositeKeys = refsetCompositeKeys;
	}

	public String getNewRF2InputFiles() {
		return newRF2InputFiles;
	}

	public void setNewRF2InputFiles(String newRF2InputFiles) {
		this.newRF2InputFiles = newRF2InputFiles;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Product)) {
			return false;
		}

		Product product = (Product) o;

		if (!businessKey.equals(product.businessKey)) {
			return false;
		}
		if (!releaseCenter.equals(product.releaseCenter)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = businessKey.hashCode();
		result = 31 * result + releaseCenter.hashCode();
		return result;
	}

	@Embeddable
	private static class RefsetCompositeKey {

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

	}

}
