package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.StringJoiner;

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

	@Column(name = "start_time")
	private Timestamp startTime;

	@Column(name = "last_updated_time")
	private Timestamp lastUpdatedTime;


	public BuildStatusTracker() {
		this.startTime = new Timestamp(System.currentTimeMillis());
		this.lastUpdatedTime = this.startTime;
	}

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
		this.lastUpdatedTime = new Timestamp(System.currentTimeMillis());
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public Timestamp getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", BuildStatusTracker.class.getSimpleName()
				+ "[", "]").add("id=" + id).add("productKey='" + productKey + "'")
				.add("releaseCenterKey='" + releaseCenterKey + "'")
				.add("buildId='" + buildId + "'")
				.add("rvfRunId='" + rvfRunId + "'")
				.add("status='" + status + "'")
				.add("startTime='" + startTime + "'")
				.add("lastUpdatedTime='" + lastUpdatedTime + "'")
				.toString();
	}
}