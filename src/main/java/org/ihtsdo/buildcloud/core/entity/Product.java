package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.hibernate.annotations.Type;
import org.hibernate.type.YesNoConverter;
import org.ihtsdo.buildcloud.core.entity.helper.EntityHelper;

import jakarta.persistence.*;

import static org.ihtsdo.buildcloud.core.entity.Build.Status;

@Entity
@Table(name="product")
@JsonPropertyOrder({"id", "name"})
public class Product {
	public static final String SNOMED_DATE_FORMAT = "yyyyMMdd";
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private long id;
	@Column(name="name")
	private String name;

	@Column(name="business_key", unique=true)
	private String businessKey;

	@ManyToOne
	@JoinColumn(name="release_center_id")
	private ReleaseCenter releaseCenter;

	@Transient
	private Status latestBuildStatus;

	@Transient
	private Build.Tag latestTag;

	@OneToOne(mappedBy="product", cascade=CascadeType.ALL)
	private BuildConfiguration buildConfiguration;

	@OneToOne(mappedBy="product", cascade=CascadeType.ALL)
	private QATestConfig qaTestConfig;

	@Convert(converter = YesNoConverter.class)
	@Column(name = "legacy_product")
	private boolean isLegacyProduct;

	@Convert(converter = YesNoConverter.class)
	@Column(name = "visibility")
	private boolean visibility;

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

	@JsonProperty
	public Build.Tag getLatestTag() {
		return latestTag;
	}

	@JsonIgnore
	public void setLatestTag(Build.Tag latestTag) {
		this.latestTag = latestTag;
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

	public boolean isLegacyProduct() {
		return isLegacyProduct;
	}

	public void setLegacyProduct(boolean legacyProduct) {
		isLegacyProduct = legacyProduct;
	}

	public boolean isVisibility() {
		return visibility;
	}

	public void setVisibility(boolean visibility) {
		this.visibility = visibility;
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
		if (!(o instanceof Product product)) {
			return false;
		}

		if (!businessKey.equals(product.businessKey)) {
			return false;
		}
		return releaseCenter.equals(product.releaseCenter);
	}

	@Override
	public int hashCode() {
		int result = businessKey.hashCode();
		result = 31 * result + releaseCenter.hashCode();
		return result;
	}

}
