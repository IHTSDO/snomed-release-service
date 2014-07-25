package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreconditionManager {

	private List <PreconditionCheck> preconditionChecks;
	private boolean onlineMode;

	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionManager.class);

	/**
	 * For each package in turn, loops through each check that has been added to the manager
	 * @return the report in a JSON friendly structure
	 */
	public Map<String, List<PreConditionCheckReport>> runPreconditionChecks(final Execution execution) {
		Map<String, List<PreConditionCheckReport>> results = new HashMap<>();
		for (Package pkg : execution.getBuild().getPackages()) {
			if (!pkg.isJustPackage()) {
				List<PreConditionCheckReport> thisPackageResults = new ArrayList<>();
				for (PreconditionCheck thisCheck : preconditionChecks) {
					if (onlineMode || !NetworkRequired.class.isAssignableFrom(thisCheck.getClass())) {
						thisCheck.runCheck(pkg, execution);
						thisPackageResults.add(thisCheck.getReport());
					} else {
						LOGGER.warn("Skipping {} as requires network.", thisCheck.getClass().getName());
					}
				}
				results.put(pkg.getName(), thisPackageResults);
			}
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

	public void setOfflineMode(boolean offlineMode) {
		this.onlineMode = !offlineMode;
	}

}
