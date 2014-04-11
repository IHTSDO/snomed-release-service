package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.User;

public interface ExtensionDAO extends EntityDAO<Extension> {

	Extension find(String releaseCenterBusinessKey, String extensionBusinessKey, User user);

}
