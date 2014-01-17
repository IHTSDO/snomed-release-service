package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.service.helper.LazyInitializer;

import java.util.List;

public interface ReleaseCentreService {

	List<ReleaseCentre> findAll();
	ReleaseCentre find(String businessKey);
	ReleaseCentre find(String businessKey, LazyInitializer<ReleaseCentre> lazyInitializer);
	void save(ReleaseCentre releaseCentre);

}
