package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.service.inputfile.gather.GatherInputRequestPojo;
import org.ihtsdo.buildcloud.core.entity.Build;

public final class CreateReleasePackageBuildRequest {

	private Build build;
	private GatherInputRequestPojo gatherInputRequestPojo;
	private String username;
	private String authenticationToken;

	public CreateReleasePackageBuildRequest() {}

	public CreateReleasePackageBuildRequest(final Build build, final GatherInputRequestPojo gatherInputRequestPojo,
	                                        final String username, final String authenticationToken) {
		this.build = build;
		this.gatherInputRequestPojo = gatherInputRequestPojo;
		this.username = username;
		this.authenticationToken = authenticationToken;
	}

	public final Build getBuild() {
		return build;
	}

	public final GatherInputRequestPojo getGatherInputRequestPojo() {
		return gatherInputRequestPojo;
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
				", username='" + username + '\'' +
				", authenticationToken='" + authenticationToken + '\'' +
				'}';
	}
}