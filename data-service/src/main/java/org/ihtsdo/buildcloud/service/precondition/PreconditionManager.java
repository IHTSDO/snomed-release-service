package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreconditionManager {

	private List <PreconditionCheck> preconditionChecks;

	/**
	 * For each package in turn, loops through each check that has been added to the manager
	 * @return the report in a JSON friendly structure
	 */
	public Map<String, Object> runPreconditionChecks(Execution execution) {
		Map<String, Object> results = new HashMap<>();
		for (Package pkg : execution.getBuild().getPackages()) {
			List<Map<PreconditionCheck.ResponseKey, String>> thisPackageResults = new ArrayList<>();
			for (PreconditionCheck thisCheck : preconditionChecks) {
				thisCheck.runCheck(pkg, execution);
				thisPackageResults.add(thisCheck.getResult());
			}
			results.put(pkg.getName(), thisPackageResults);
		}
		return results;
	}

	public PreconditionManager preconditionChecks(PreconditionCheck... preconditionCheckArray) {
		List<PreconditionCheck> preconditionChecks = new ArrayList<>();
		for (PreconditionCheck check : preconditionCheckArray) {
			preconditionChecks.add(check);
		}
		this.preconditionChecks = preconditionChecks;
		return this;
	}

	public void setPreconditionChecks(List<PreconditionCheck> preconditionChecks) {
		this.preconditionChecks = preconditionChecks;
	}

}
