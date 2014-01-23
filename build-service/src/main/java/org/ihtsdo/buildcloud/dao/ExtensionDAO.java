package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Extension;

public interface ExtensionDAO {

	Extension find(String releaseCentreBusinessKey, String extensionBusinessKey, String oauthId);

}
