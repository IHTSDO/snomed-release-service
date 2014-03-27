package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;

import java.util.List;

public interface ExtensionService extends EntityService<Extension> {

	List<Extension> findAll(String releaseCenterBusinessKey, String oauthId);
	Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, String oauthId);
	Extension create(String releaseCenterBusinessKey, String name, String authenticatedId);

}
