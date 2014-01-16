package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;

import java.util.List;

public interface ReleaseCentreDAO {
	List<ReleaseCentre> getReleaseCentres();
	void save(ReleaseCentre releaseCentre);
}
