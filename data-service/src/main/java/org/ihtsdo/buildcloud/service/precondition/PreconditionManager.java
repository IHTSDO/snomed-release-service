package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PreconditionManager {

	private List<PreconditionCheck> preconditionChecks;

	private boolean onlineMode;

	@Value("${localRvf}")
	private Boolean localRvf;

	private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionManager.class);

	/**
	 * Runs each PreconditionCheck which has been added to the manager.
	 *
	 * @return the report in a JSON friendly structure
	 */
	public List<PreConditionCheckReport> runPreconditionChecks(final Build build) {
		List<PreConditionCheckReport> checkReports = new ArrayList<>();
		for (PreconditionCheck thisCheck : preconditionChecks) {
			if (onlineMode || !NetworkRequired.class.isAssignableFrom(thisCheck.getClass())
					|| (RF2FilesCheck.class.isAssignableFrom(thisCheck.getClass()) && localRvf)) {
				if (thisCheck instanceof TermServerClassificationResultsCheck && !build.getConfiguration().useClassifierPreConditionChecks() ) {
					continue;
				}
				thisCheck.runCheck(build);
				checkReports.add(thisCheck.getReport());
			} else {
				LOGGER.warn("Skipping {} as requires network.", thisCheck.getClass().getName());
			}
		}
		return checkReports;
	}

	public PreconditionManager preconditionChecks(PreconditionCheck... preconditionCheckArray) {
		List<PreconditionCheck> preconditionChecks = new ArrayList<>();
		Collections.addAll(preconditionChecks, preconditionCheckArray);
		this.preconditionChecks = preconditionChecks;
		return this;
	}

	public void setPreconditionChecks(List<PreconditionCheck> preconditionChecks) {
		this.preconditionChecks = preconditionChecks;
	}

	public void setOfflineMode(@Value("${offlineMode}") boolean offlineMode) {
		this.onlineMode = !offlineMode;
	}

}
