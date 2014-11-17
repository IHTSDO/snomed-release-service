package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import java.util.*;
import javax.persistence.*;

@Entity
@JsonPropertyOrder({"id", "name"})
public class Package implements Comparable<Package> {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	@JsonProperty("id")
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private Build build;

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

	public Package() {
		inputFiles = new ArrayList<>();
	}

	public Package(String name) {
		this();
		setName(name);
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

	public void setName(String name) {
		this.name = name;
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public Build getBuild() {
		return build;
	}

	public void setBuild(Build build) {
		this.build = build;
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

	public boolean isJustPackage() {
		return justPackage;
	}

	public void setJustPackage(boolean justPackage) {
		this.justPackage = justPackage;
	}

	public void setPreviousPublishedPackage(String previousPublishedFileName) {
		previousPublishedPackage = previousPublishedFileName;
	}

	public String getPreviousPublishedPackage() {
		return previousPublishedPackage;
	}

	public boolean isWorkbenchDataFixesRequired() {
		return workbenchDataFixesRequired;
	}

	public void setWorkbenchDataFixesRequired(boolean workbenchDataFixesRequired) {
		this.workbenchDataFixesRequired = workbenchDataFixesRequired;
	}

	public void setCreateInferredRelationships(boolean createInferredRelationships) {
		this.createInferredRelationships = createInferredRelationships;
	}

	public boolean isCreateInferredRelationships() {
		return createInferredRelationships;
	}

	public void setCustomRefsetCompositeKeys(Map<String, List<Integer>> customRefsetCompositeKeys) {
		this.refsetCompositeKeys = toRefsetCompositeKeys(customRefsetCompositeKeys);
	}

	private Set<RefsetCompositeKey> toRefsetCompositeKeys(Map<String, List<Integer>> customRefsetCompositeKeys) {
		HashSet<RefsetCompositeKey> keys = new HashSet<>();
		for (String key : customRefsetCompositeKeys.keySet()) {
			keys.add(new RefsetCompositeKey(key, customRefsetCompositeKeys.get(key).toString().replaceAll("[\\[\\]]", "")));
		}
		return keys;
	}

	public Map<String, List<Integer>> getCustomRefsetCompositeKeys() {
		HashMap<String, List<Integer>> map = new HashMap<>();
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

	public void setNewRF2InputFiles(String newRF2InputFiles) {
		this.newRF2InputFiles = newRF2InputFiles;
	}

	public Set<String> getNewRF2InputFileSet() {
		Set<String> files = new HashSet<>();
		if (newRF2InputFiles != null) {
			Collections.addAll(files, newRF2InputFiles.split("\\|"));
		}
		return files;
	}

	@Override
	public int compareTo(Package aPackage) {
		return this.getBusinessKey().compareTo(aPackage.getBusinessKey());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Package)) return false;

		Package aPackage = (Package) o;

		if (!build.equals(aPackage.build)) return false;
		if (!businessKey.equals(aPackage.businessKey)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = businessKey.hashCode();
		result = 31 * result + build.hashCode();
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
