package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.Build;

public final class CreateReleasePackageBuildRequest {

    private Build build;
    private String username;
    private String authenticationToken;

    public CreateReleasePackageBuildRequest() {
    }

    public CreateReleasePackageBuildRequest(final Build build, final String username, final String authenticationToken) {
        this.build = build;
        this.username = username;
        this.authenticationToken = authenticationToken;
    }

    public Build getBuild() {
        return build;
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
                "build=" + build.toString() +
                ", username='" + username + '\'' +
                ", authenticationToken='" + (authenticationToken != null ? authenticationToken.substring(0, Math.min(authenticationToken.length(), authenticationToken.indexOf("=") + 6)) + "....." : null) + '\'' +
                '}';
    }
}