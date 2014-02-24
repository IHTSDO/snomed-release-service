package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;

import java.util.List;

public interface ExtensionService extends EntityService<Extension> {

	List<Extension> findAll(String releaseCentreBusinessKey, String oauthId);
	Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId);

}
