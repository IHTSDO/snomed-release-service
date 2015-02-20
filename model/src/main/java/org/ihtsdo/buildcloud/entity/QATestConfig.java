package org.ihtsdo.buildcloud.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name="qa_config")
public class QATestConfig {
	@Id
	@GeneratedValue
	@JsonIgnore
	@Column(name="id")
	private long id;
	
	@OneToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;
	
	@Column(name="assertion_group_names")
	private String assertionGroupNames;
	
	@Column(name="previous_int_release")
	private String previousInternationalRelease;
	
	@Column(name="previous_ext_release")
	private String previousExtensionRelease;
	
	@Column(name="extension_baseline_release")
	private String extensionBaseLineRelease;
	
	public String getAssertionGroupNames() {
		return assertionGroupNames;
	}
	public void setAssertionGroupNames(final String assertionGroupNames) {
		this.assertionGroupNames = assertionGroupNames;
	}
	
	public String getPreviousInternationalRelease() {
		return previousInternationalRelease;
	}
	public void setPreviousInternationalRelease(final String previousIntRelease) {
		this.previousInternationalRelease = previousIntRelease;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(final Product product) {
		this.product = product;
	}
	public String getPreviousExtensionRelease() {
		return previousExtensionRelease;
	}
	public void setPreviousExtensionRelease(final String previousExtensionRelease) {
		this.previousExtensionRelease = previousExtensionRelease;
	}
	public String getExtensionBaseLineRelease() {
		return extensionBaseLineRelease;
	}
	public void setExtensionBaseLineRelease(final String extBaseLineRelease) {
		extensionBaseLineRelease = extBaseLineRelease;
	}
	@Override
	public String toString() {
		return "QATestConfig [id=" + id + ", assertionGroupNames="
				+ assertionGroupNames + ", previousInternationalRelease="
				+ previousInternationalRelease + ", previousExtensionRelease="
				+ previousExtensionRelease + ", extensionBaseLineRelease="
				+ extensionBaseLineRelease + "]";
	}
}
