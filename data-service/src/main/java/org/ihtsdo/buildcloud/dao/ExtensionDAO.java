package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;

public interface ExtensionDAO extends EntityDAO<Extension> {

	Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, String authenticatedId);

}
