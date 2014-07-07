package org.ihtsdo.buildcloud.service.precondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

public class PreconditionManager {
	
	private List <PreconditionCheck> preconditionChecksToRun;
	
	//Package level modifier as the unit tests will need to change the execution at runtime
	Execution execution;
	
	private PreconditionManager(Execution execution) {
		preconditionChecksToRun = new ArrayList<PreconditionCheck>();
		this.execution = execution;
	}
	
	public static PreconditionManager build(Execution execution){
		return new PreconditionManager(execution);
	}
	
	public PreconditionManager add (PreconditionCheck pcc) {
		pcc.setManager(this);
		this.preconditionChecksToRun.add(pcc);
		return this;
	}
	
	/**
	 * For each package in turn, loops through each check that has been added to the manager
	 * @return the report in a JSON friendly structure
	 */
	public Map<String, Object> runPreconditionChecks() {

		@SuppressWarnings("unchecked")  //Triple generics is just too unwieldy to allow out of here, also would require dependency for model on data-service
		Map<String, Object> results = Map.class.cast(new HashMap<String, List<Map<PreconditionCheck.ResponseKey, String>>>());
		for ( Package pkg : execution.getBuild().getPackages()) {
			List<Map<PreconditionCheck.ResponseKey, String>> thisPackageResults = new ArrayList<Map<PreconditionCheck.ResponseKey, String>>();
			for (PreconditionCheck thisCheck : preconditionChecksToRun) {
				thisCheck.runCheck(pkg);
				thisPackageResults.add(thisCheck.getResult());
			}
			results.put (pkg.getName(), thisPackageResults);
		}
		return results;
	}

}
