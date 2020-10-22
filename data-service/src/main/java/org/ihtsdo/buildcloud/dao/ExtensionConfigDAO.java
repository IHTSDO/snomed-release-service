package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ExtensionConfig;
import org.ihtsdo.buildcloud.entity.Product;
import org.ihtsdo.buildcloud.service.helper.FilterOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface ExtensionConfigDAO extends EntityDAO<ExtensionConfig> {
}
