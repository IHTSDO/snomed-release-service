package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Release;

import java.util.Set;

public interface ReleaseService {

	Set<Release> findAll(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String authenticatedId);

	Release find(String releaseCentreBusinessKey, String extensionBusinessKey, String productBusinessKey, String releaseBusinessKey, String authenticatedId);

}
