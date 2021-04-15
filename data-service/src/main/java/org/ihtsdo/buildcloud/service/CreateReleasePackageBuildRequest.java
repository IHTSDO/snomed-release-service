package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.springframework.security.core.Authentication;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private GatherInputRequestPojo gatherInputRequestPojo;
	private String rootUrl;
	private Authentication authentication;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final GatherInputRequestPojo gatherInputRequestPojo,
			final String rootUrl, final Authentication authentication) {
		this.build = build;
		this.gatherInputRequestPojo = gatherInputRequestPojo;
		this.rootUrl = rootUrl;
		this.authentication = authentication;
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

	public final Authentication getAuthentication() {
		return authentication;
	}
}