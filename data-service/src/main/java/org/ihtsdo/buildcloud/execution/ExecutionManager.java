package org.ihtsdo.buildcloud.execution;

import java.io.IOException;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.springframework.beans.factory.annotation.Autowired;

public class ExecutionManager {
	
	@Autowired
	private ExecutionDAO dao;
	
	private Execution execution; 
	
	public ExecutionManager (Execution execution) {
		this.execution = execution;
	}
	
	public Execution doExecution() throws IOException{
		
		String executionConfiguration = dao.loadConfiguration(execution);
		
		// Replace Effective Dates
		
		// Get IDs as required from ID Generator
		
		// Run classifier

		// Do packaging
		
		// Call Release Verification Framework
		
		return this.execution;
	}

}
