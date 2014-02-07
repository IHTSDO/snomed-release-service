package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

import javax.persistence.*;

import java.util.*;

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
	private List<Package> packages;
	
	public Build() {
		packages = new ArrayList<>();
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

	public List<Package> getPackages() {
		return packages;
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	@JsonIgnore
	public Map<String, Object> getConfig() {
		Map<String, Object> config = new HashMap<String, Object>();
		ArrayList<Map<String,Object>> packagesConfig = new ArrayList<Map<String,Object>>();

		for (Package pkg : getPackages()) {
			packagesConfig.add(pkg.getConfig());
		}
		config.put("Packages", packagesConfig);
		return config;
	}
}
