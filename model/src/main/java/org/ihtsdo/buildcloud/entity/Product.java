package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;

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

	@JsonIgnore
	private String businessKey;

	@ManyToOne
	@JsonIgnore
	private ReleaseCenter releaseCenter;

	private BuildConfiguration buildConfiguration;

	public Product() {
		buildConfiguration = new BuildConfiguration();
	}

	public Product(String name) {
		this();
		setName(name);
	}
	
	public Product(Long id, String name) {
		this(name);
		this.id = id;
	}

	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(BuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
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

	public int hashCode() {
		int result = businessKey.hashCode();
		result = 31 * result + releaseCenter.hashCode();
		return result;
	}

}
