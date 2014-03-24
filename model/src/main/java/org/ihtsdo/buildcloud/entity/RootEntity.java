package org.ihtsdo.buildcloud.entity;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize
public class RootEntity implements DomainEntity {

	@Override
	public DomainEntity getParent() {
		return null;
	}

	@Override
	public String getCollectionName() {
		return null;
	}

	@Override
	public String getBusinessKey() {
		return null;
	}

}
