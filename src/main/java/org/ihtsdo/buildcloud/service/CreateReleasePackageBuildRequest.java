package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private GatherInputRequestPojo gatherInputRequestPojo;
	private String rootUrl;
	private String username;
	private String authenticationToken;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final GatherInputRequestPojo gatherInputRequestPojo,
			final String rootUrl, final String username, final String authenticationToken) {
		this.build = build;
		this.gatherInputRequestPojo = gatherInputRequestPojo;
		this.rootUrl = rootUrl;
		this.username = username;
		this.authenticationToken = authenticationToken;
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

	public String getUsername() {
		return username;
	}

	public String getAuthenticationToken() {
		return authenticationToken;
	}

	@Override
	public String toString() {
		return "CreateReleasePackageBuildRequest{" +
				"build=" + build +
				", gatherInputRequestPojo=" + gatherInputRequestPojo +
				", rootUrl='" + rootUrl + '\'' +
				", username='" + username + '\'' +
				", authenticationToken='" + authenticationToken + '\'' +
				'}';
	}
}