package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import org.hibernate.type.YesNoConverter;

@Entity
@Table(name="qa_config")
public class QATestConfig {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private long id;
	
	@OneToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;
	
	@Column(name="assertion_group_names")
	private String assertionGroupNames;

	@Column(name="drools_rules_group_names")
	private String droolsRulesGroupNames;
	
	@Column(name = "storage_location")
	private String storageLocation;

	@Convert(converter = YesNoConverter.class)
	@Column(name = "enable_drools")
	private boolean enableDrools = false;

	@Convert(converter = YesNoConverter.class)
	@Column(name = "enable_mrcm_validation")
	private boolean enableMRCMValidation;

	@Transient
	private Integer maxFailureExport;

	@Transient
	private boolean enableTraceabilityValidation;

	@Transient
	private Long contentHeadTimestamp;

	public String getAssertionGroupNames() {
		return assertionGroupNames;
	}
	public void setAssertionGroupNames(final String assertionGroupNames) {
		this.assertionGroupNames = assertionGroupNames;
	}

	public String getDroolsRulesGroupNames() {
		return droolsRulesGroupNames;
	}

	public void setDroolsRulesGroupNames(String droolsRulesGroupNames) {
		this.droolsRulesGroupNames = droolsRulesGroupNames;
	}

	public Product getProduct() {
		return product;
	}
	public void setProduct(final Product product) {
		this.product = product;
	}

	public boolean isEnableDrools() {
		return enableDrools;
	}

	public void setEnableDrools(boolean enableDrools) {
		this.enableDrools = enableDrools;
	}

	@Override
	public String toString() {
		return "QATestConfig{" +
				"id=" + id +
				", product=" + (product != null ? product.getBusinessKey() : null) +
				", assertionGroupNames='" + assertionGroupNames + '\'' +
				", droolsRulesGroupNames='" + droolsRulesGroupNames + '\'' +
				", storageLocation='" + storageLocation + '\'' +
				", enableDrools=" + enableDrools +
				", enableMRCMValidation=" + enableMRCMValidation +
				", maxFailureExport=" + maxFailureExport +
				", enableTraceabilityValidation=" + enableTraceabilityValidation +
				", contentHeadTimestamp=" + contentHeadTimestamp +
				'}';
	}

	public String getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(final String storageLocation) {
		this.storageLocation = storageLocation;
	}

	public boolean isEnableMRCMValidation() {
		return enableMRCMValidation;
	}

	public void setEnableMRCMValidation(boolean enableMRCMValidation) {
		this.enableMRCMValidation = enableMRCMValidation;
	}

	public Integer getMaxFailureExport() {
		return maxFailureExport;
	}

	public void setMaxFailureExport(Integer maxFailureExport) {
		this.maxFailureExport = maxFailureExport;
	}

	public void setEnableTraceabilityValidation(boolean enableTraceabilityValidation) {
		this.enableTraceabilityValidation = enableTraceabilityValidation;
	}

	public boolean isEnableTraceabilityValidation() {
		return enableTraceabilityValidation;
	}

	public void setContentHeadTimestamp(Long contentHeadTimestamp) {
		this.contentHeadTimestamp = contentHeadTimestamp;
	}

	public Long getContentHeadTimestamp() {
		return contentHeadTimestamp;
	}
}
