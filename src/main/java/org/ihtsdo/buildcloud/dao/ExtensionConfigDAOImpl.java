package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.springframework.stereotype.Service;

@Service
public class ExtensionConfigDAOImpl extends EntityDAOImpl<ExtensionConfig> implements ExtensionConfigDAO {

    public ExtensionConfigDAOImpl() {
        super(ExtensionConfig.class);
    }
}
