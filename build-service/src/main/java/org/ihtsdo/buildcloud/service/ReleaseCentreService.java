package org.ihtsdo.buildcloud.service;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreService {
	List<ReleaseCentre> getReleaseCentres();
	void save(ReleaseCentre releaseCentre);
}
