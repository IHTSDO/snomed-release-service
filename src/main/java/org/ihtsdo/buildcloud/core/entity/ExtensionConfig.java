package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="extension_config")
public class ExtensionConfig implements Serializable {
	
	private static final long serialVersionUID = 2315869111370910808L;

	@Id
	@OneToOne
	@JoinColumn(name="build_config_id")
	@JsonIgnore
	private BuildConfiguration buildConfiguration;
	
	@Column(name="namespace_id")
	private String namespaceId;
	
	@Column(name="module_id")
	private String moduleId;
	
	@Column(name="dependency_release")
	private String dependencyRelease;
	
	@Type(type="yes_no")
	@Column(name="release_as_edition")
	private boolean releaseAsAnEdition;

	public String getDependencyRelease() {
		return dependencyRelease;
	}

	public void setDependencyRelease(String dependencyRelease) {
		this.dependencyRelease = dependencyRelease;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(BuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
	}
	
	public boolean isReleaseAsAnEdition() {
		return this.releaseAsAnEdition;
	}
	
	public void setReleaseAsAnEdition(boolean releaseExtensionAsAnEdition) {
		this.releaseAsAnEdition = releaseExtensionAsAnEdition;
	}

}
