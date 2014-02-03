package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Build;

import java.util.List;

public interface BuildService {

	List<Build> findAll(String authenticatedId);

	Build find(String buildCompositeKey, String authenticatedId);
}
