package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name="rvf_failure_jira_associations")
public class RVFFailureJiraAssociation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private Long id;

	@ManyToOne
	@JoinColumn(name="release_center_id")
	@JsonIgnore
	private ReleaseCenter releaseCenter;

	@ManyToOne
	@JoinColumn(name="product_id")
	@JsonIgnore
	private Product product;

	@Column(name="build_id")
	private String buildId;

	@Column(name="assertion_id")
	private String assertionId;

	@Column(name="jira_url")
	private String jiraUrl;

	public RVFFailureJiraAssociation() {
	}

	public RVFFailureJiraAssociation(ReleaseCenter releaseCenter, Product product, String buildId, String assertionId, String jiraUrl) {
		this.releaseCenter = releaseCenter;
		this.product = product;
		this.buildId = buildId;
		this.assertionId = assertionId;
		this.jiraUrl = jiraUrl;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ReleaseCenter getReleaseCenter() {
		return releaseCenter;
	}

	public void setReleaseCenter(ReleaseCenter releaseCenter) {
		this.releaseCenter = releaseCenter;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public String getBuildId() {
		return buildId;
	}

	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	public String getAssertionId() {
		return assertionId;
	}

	public void setAssertionId(String assertionId) {
		this.assertionId = assertionId;
	}

	public String getJiraUrl() {
		return jiraUrl;
	}

	public void setJiraUrl(String jiraUrl) {
		this.jiraUrl = jiraUrl;
	}
}