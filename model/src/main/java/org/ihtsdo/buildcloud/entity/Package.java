package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Package {

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

	@OneToMany(mappedBy = "packag")
	@JsonIgnore
	private Set<InputFile> inputFiles;

	public Package() {
		inputFiles = new HashSet<>();
	}

	public Package(String name) {
		this();
		setName(name);
	}

	public void addInputFile(InputFile inputFile) {
		inputFiles.add(inputFile);
		inputFile.setPackage(this);
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

	public Set<InputFile> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(Set<InputFile> inputFiles) {
		this.inputFiles = inputFiles;
	}
}
