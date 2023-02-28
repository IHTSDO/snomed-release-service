package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

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

	@Column(name="previous_edition_dependency_effective_date")
	private Date previousEditionDependencyEffectiveDate; // yyyy-mm-dd format

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

	public Date getPreviousEditionDependencyEffectiveDate() {
		return this.previousEditionDependencyEffectiveDate;
	}

	@JsonProperty("previousEditionDependencyEffectiveDate")
	public String getPreviousEditionDependencyEffectiveDateFormatted() {
		return previousEditionDependencyEffectiveDate != null ? DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(previousEditionDependencyEffectiveDate) : null;
	}

	public void setPreviousEditionDependencyEffectiveDate(String previousEditionDependencyEffectiveDate) throws ParseException {
		if (previousEditionDependencyEffectiveDate != null) {
			this.previousEditionDependencyEffectiveDate = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.parse(previousEditionDependencyEffectiveDate);
		}
	}

	public void setPreviousEditionDependencyEffectiveDate(Date previousEditionDependencyEffectiveDate) {
		this.previousEditionDependencyEffectiveDate = previousEditionDependencyEffectiveDate;
	}
}
