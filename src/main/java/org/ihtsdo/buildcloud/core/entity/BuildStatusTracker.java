package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "build_status_tracker")
public class BuildStatusTracker {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name = "id")
	private long id;

	@Column(name = "product_key")
	private String productKey;

	@Column(name = "release_center_key")
	private String releaseCenterKey;

	@Column(name = "build_id")
	private String buildId;

	@Column(name = "rvf_run_id", unique = true)
	private String rvfRunId;

	@Column(name = "status")
	private String status;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getProductKey() {
		return productKey;
	}

	public void setProductKey(String productKey) {
		this.productKey = productKey;
	}

	public String getReleaseCenterKey() {
		return releaseCenterKey;
	}

	public void setReleaseCenterKey(String releaseCenterKey) {
		this.releaseCenterKey = releaseCenterKey;
	}

	public String getBuildId() {
		return buildId;
	}

	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}

	public String getRvfRunId() {
		return rvfRunId;
	}

	public void setRvfRunId(String rvfRunId) {
		this.rvfRunId = rvfRunId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}