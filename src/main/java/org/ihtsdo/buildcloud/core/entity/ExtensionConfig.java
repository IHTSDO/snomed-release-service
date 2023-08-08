package org.ihtsdo.buildcloud.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name="extension_config")
public class ExtensionConfig implements Serializable {
	
	@Serial
	private static final long serialVersionUID = 2315869111370910808L;

	@Id
	@OneToOne
	@JoinColumn(name="build_config_id")
	@JsonIgnore
	private BuildConfiguration buildConfiguration;
	
	@Column(name="namespace_id")
	private String namespaceId;

	@Column(name="default_module_id")
	private String defaultModuleId;

	@Column(name="module_ids")
	private String moduleIds;
	
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

	public String getDefaultModuleId() {
		return defaultModuleId;
	}

	public void setDefaultModuleId(String defaultModuleId) {
		this.defaultModuleId = defaultModuleId;
	}

	public Set<String> getModuleIdsSet() {
		if (moduleIds == null) return null;
		return Arrays.stream(moduleIds.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	public String getModuleIds() {
		return moduleIds;
	}

	public void setModuleIds(String moduleIds) {
		this.moduleIds = moduleIds;
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
