package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Product implements DomainEntity{

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	@JsonProperty("id")
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private Extension extension;

	@OneToMany(mappedBy = "product")
	@JsonIgnore
	private List<Build> builds;

	public Product() {
		builds = new ArrayList<>();
	}

	public Product(String name) {
		this();
		setName(name);
	}

	public void addBuild(Build build) {
		builds.add(build);
		build.setProduct(this);
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

	public Extension getExtension() {
		return extension;
	}

	public void setExtension(Extension extension) {
		this.extension = extension;
	}

	public List<Build> getBuilds() {
		return builds;
	}

	@Override
	public DomainEntity getParent() {
		return extension;
	}

	@Override
	public String getCollectionName() {
		return "products";
	}	
	
}
