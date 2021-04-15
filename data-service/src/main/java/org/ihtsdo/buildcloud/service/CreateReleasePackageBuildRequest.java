package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;

public final class CreateReleasePackageBuildRequest {

	private final Build build;
	private final GatherInputRequestPojo gatherInputRequestPojo;
	private final String rootUrl;

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