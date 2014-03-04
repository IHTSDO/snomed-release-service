package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;

import java.util.ArrayList;

public interface ExecutionDAO {
	void save(Execution execution);

	ArrayList<Execution> findAll(Build build);

	Execution find(Build build, String executionId);

}
