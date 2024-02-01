package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name="build_additional_package_mapping")
public class BuildAdditionalPackage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonIgnore
	@Column(name="id")
	private long id;

	@ManyToOne
	@JoinColumn(name="build_config_id")
	@JsonIgnore
	private BuildConfiguration buildConfiguration;

	@Column(name="release_center_key")
	private String releaseCenterKey;

	@Column(name="additional_package_name")
	private String additionalPackageName;

	public long getId() {
		return id;
	}

	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(BuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
	}

	public String getReleaseCenterKey() {
		return releaseCenterKey;
	}

	public void setReleaseCenterKey(String releaseCenterKey) {
		this.releaseCenterKey = releaseCenterKey;
	}

	public String getAdditionalPackageName() {
		return additionalPackageName;
	}

	public void setAdditionalPackageName(String additionalPackageName) {
		this.additionalPackageName = additionalPackageName;
	}

	@Override
	public String toString() {
		return "BuildAdditionalPackage{" +
				"id=" + id +
				", buildConfigurationId=" + buildConfiguration.getId() +
				", releaseCenterKey='" + releaseCenterKey + '\'' +
				", additionalPackageName='" + additionalPackageName + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BuildAdditionalPackage that)) return false;
        return id == that.id && Objects.equals(buildConfiguration, that.buildConfiguration) && Objects.equals(releaseCenterKey, that.releaseCenterKey) && Objects.equals(additionalPackageName, that.additionalPackageName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, buildConfiguration, releaseCenterKey, additionalPackageName);
	}
}
