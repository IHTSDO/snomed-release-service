package org.ihtsdo.buildcloud.core.dao;

import org.ihtsdo.buildcloud.core.entity.ExtensionConfig;
import org.springframework.stereotype.Service;

@Service
public class ExtensionConfigDAOImpl extends EntityDAOImpl<ExtensionConfig> implements ExtensionConfigDAO {

    public ExtensionConfigDAOImpl() {
        super(ExtensionConfig.class);
    }
}
