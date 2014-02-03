package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Build {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;

	private String name;

	@JsonIgnore
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private Product product;

	@OneToMany(mappedBy = "build")
	@JsonIgnore
	private Set<Package> packages;
	
	private String config;

	public Build() {
		packages = new HashSet<>();
	}

	public Build(String name) {
		this();
		setName(name);
	}

	public void addPackage(Package aPackage) {
		packages.add(aPackage);
		aPackage.setBuild(this);
	}

	@JsonProperty("id")
	public String getCompositeKey() {
		return id + "_" + businessKey;
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
		generateBusinessKey();
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Set<Package> getPackages() {
		return packages;
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public String getConfig() {
		//return config;
		return "Server side config to go here";
	}

	public void setConfig(String config) {
		this.config = config;
	}
}
