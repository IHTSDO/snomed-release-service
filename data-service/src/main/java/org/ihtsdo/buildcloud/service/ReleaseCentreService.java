package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreService {

	List<ReleaseCentre> findAll(String oauthId);

	ReleaseCentre find(String businessKey, String oauthId);

}
