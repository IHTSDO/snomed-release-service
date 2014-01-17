package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.Extension;
import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ExtensionService {

	List<Extension> findAll();
	Extension find(String businessKey);
	void save(Extension releaseCentre);

}
