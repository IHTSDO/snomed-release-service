package org.ihtsdo.buildcloud.service.execution;

import org.ihtsdo.buildcloud.dao.ExecutionDAO;
import org.ihtsdo.buildcloud.entity.Execution;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class ExecutionManager {
	
	@Autowired
	private ExecutionDAO dao;
	
	private Execution execution; 
	
	public ExecutionManager (Execution execution) {
		this.execution = execution;
	}
	
	public Execution doExecution() throws IOException {
		
		String executionConfiguration = dao.loadConfiguration(execution);
		
		// Replace Effective Dates
		
		// Get IDs as required from ID Generator
		
		// Run classifier
			// (Not required for this epic)

		// Do packaging
		
		// Call Release Verification Framework
		
		return this.execution;
	}

}
