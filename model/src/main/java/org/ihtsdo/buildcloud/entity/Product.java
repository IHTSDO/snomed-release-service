package org.ihtsdo.buildcloud.entity;

import javax.persistence.*;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.ihtsdo.buildcloud.entity.helper.EntityHelper;
import static org.ihtsdo.buildcloud.entity.Build.Status;

@Entity
@Table(name="product")
@JsonPropertyOrder({"id", "name"})
public class Product {
	public static final String SNOMED_DATE_FORMAT = "yyyyMMdd";
	@Id
	@GeneratedValue
	@JsonIgnore
	@Column(name="id")
	private long id;
	@Column(name="name")
	private String name;

	@JsonIgnore
	@Column(name="business_key", unique=true)
	private String businessKey;

	@ManyToOne
	@JoinColumn(name="release_center_id")
	@JsonIgnore
	private ReleaseCenter releaseCenter;

	@JsonIgnore
	@Transient
	private Status latestBuildStatus;

	@OneToOne(mappedBy="product", cascade=CascadeType.ALL)
	private BuildConfiguration buildConfiguration;

	@OneToOne(mappedBy="product", cascade=CascadeType.ALL)
	private QATestConfig qaTestConfig;

	public Product() {
	}

	public Product(final String name) {
		this();
		setName(name);
	}

	public QATestConfig getQaTestConfig() {
		return qaTestConfig;
	}

	public void setQaTestConfig(final QATestConfig qaTestConfig) {
		this.qaTestConfig = qaTestConfig;
	}

	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(final BuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
	}

	public void setName(final String name) {
		this.name = name;
		generateBusinessKey();
	}

	private void generateBusinessKey() {
		this.businessKey = EntityHelper.formatAsBusinessKey(name);
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	@JsonProperty("id")
	public String getBusinessKey() {
		return businessKey;
	}


	@JsonProperty
	public Status getLatestBuildStatus() {
		return latestBuildStatus;
	}

	@JsonIgnore
	public void setLatestBuildStatus(Status latestBuildStatus) {
		this.latestBuildStatus = latestBuildStatus;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(final ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public void setBusinessKey(final String businessKey) {
		this.businessKey = businessKey;
	}
	
	@Override
	public String toString() {
		return "Product [id=" + id + ", name=" + name + ", businessKey="
				+ businessKey + ", releaseCenter=" + releaseCenter
				+ ", buildConfiguration=" + buildConfiguration
				+ ", qaTestConfig=" + qaTestConfig + "]";
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Product)) {
			return false;
		}

		final Product product = (Product) o;

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

}
