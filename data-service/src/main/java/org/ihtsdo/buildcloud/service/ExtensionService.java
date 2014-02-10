package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;

import java.util.Set;

public interface ExtensionService {

	Set<Extension> findAll(String releaseCentreBusinessKey, String oauthId);
	Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId);

}
