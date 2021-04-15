package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.springframework.security.core.context.SecurityContextImpl;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private GatherInputRequestPojo gatherInputRequestPojo;
	private String rootUrl;
	private SecurityContextImpl securityContext;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final GatherInputRequestPojo gatherInputRequestPojo,
			final String rootUrl, final SecurityContextImpl securityContext) {
		this.build = build;
		this.gatherInputRequestPojo = gatherInputRequestPojo;
		this.rootUrl = rootUrl;
		this.securityContext = securityContext;
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

	public final SecurityContextImpl getSecurityContext() {
		return securityContext;
	}
}