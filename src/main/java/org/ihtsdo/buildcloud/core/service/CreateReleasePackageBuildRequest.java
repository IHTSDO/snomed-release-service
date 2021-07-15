package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.service.inputfile.gather.BuildRequestPojo;
import org.ihtsdo.buildcloud.core.entity.Build;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private BuildRequestPojo buildRequestPojo;
	private String username;
	private String authenticationToken;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final BuildRequestPojo buildRequestPojo,
										final String username, final String authenticationToken) {
		this.build = build;
		this.buildRequestPojo = buildRequestPojo;
		this.username = username;
		this.authenticationToken = authenticationToken;
	}

	public final Build getBuild() {
		return build;
	}

	public final BuildRequestPojo getBuildRequestPojo() {
		return buildRequestPojo;
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
				", buildRequestPojo=" + buildRequestPojo +
				", username='" + username + '\'' +
				", authenticationToken='" + authenticationToken + '\'' +
				'}';
	}
}