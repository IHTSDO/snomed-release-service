package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.User;
import org.ihtsdo.buildcloud.service.helper.FilterOption;

import java.util.List;
import java.util.Set;

public interface BuildDAO extends EntityDAO<Build> {

	List<Build> findAll(Set<FilterOption> filterOptions, User user);

	List<Build> findAll(String releaseCenterBusinessKey, Set<FilterOption> filterOptions, User user);

	Build find(String releaseCenterKey, String buildKey, User user);

	Build find(Long id, User user);

}
