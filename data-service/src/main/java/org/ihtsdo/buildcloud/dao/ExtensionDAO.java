package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;

public interface ExtensionDAO extends EntityDAO<Extension> {

	Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String authenticatedId);

}
