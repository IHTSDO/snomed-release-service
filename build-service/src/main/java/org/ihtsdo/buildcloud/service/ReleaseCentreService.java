package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.service.helper.LazyInitializer;

import java.util.List;
import java.util.Set;

public interface ReleaseCentreService {

	List<ReleaseCentre> findAll(String oauthId);

	ReleaseCentre find(String businessKey, String oauthId);

	ReleaseCentre find(String businessKey, LazyInitializer<ReleaseCentre> lazyInitializer, String oauthId);

	void save(ReleaseCentre releaseCentre);

}
