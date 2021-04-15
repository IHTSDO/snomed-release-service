package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private GatherInputRequestPojo gatherInputRequestPojo;
	private String rootUrl;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final GatherInputRequestPojo gatherInputRequestPojo, final String rootUrl) {
		this.build = build;
		this.gatherInputRequestPojo = gatherInputRequestPojo;
		this.rootUrl = rootUrl;
	}

	public final Build getBuild() {
		return build;
	}

	public final GatherInputRequestPojo getGatherInputRequestPojo() {
		return gatherInputRequestPojo;
	}

	public final String getRootUrl() {
		return rootUrl;
	}
}