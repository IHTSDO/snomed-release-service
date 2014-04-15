package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.annotate.JsonIgnore;

public interface DomainEntity {

	@JsonIgnore
	public DomainEntity getParent();

	@JsonIgnore
	public String getCollectionName();

	@JsonIgnore
	public String getBusinessKey();
}
