package org.ihtsdo.buildcloud.dao;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface ExecutionDAO {

	void save(Execution execution, String jsonConfig);

	ArrayList<Execution> findAll(Build build);

	Execution find(Build build, String executionId);

	String loadConfiguration(Execution execution) throws IOException;

	void saveBuildScripts(File buildScriptsTmpDirectory, Execution execution);
}
