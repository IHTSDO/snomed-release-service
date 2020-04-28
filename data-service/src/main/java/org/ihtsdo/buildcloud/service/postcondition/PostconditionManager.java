package org.ihtsdo.buildcloud.service.postcondition;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.PostConditionCheckReport;
import org.ihtsdo.buildcloud.entity.PreConditionCheckReport;
import org.ihtsdo.buildcloud.service.NetworkRequired;
import org.ihtsdo.buildcloud.service.precondition.PreconditionCheck;
import org.ihtsdo.buildcloud.service.precondition.RF2FilesCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class PostconditionManager {

	private List<PostconditionCheck> postconditionChecks;

	private static final Logger LOGGER = LoggerFactory.getLogger(PostconditionManager.class);

	public List<PostConditionCheckReport> runPostconditionChecks(final Build build) {
		List<PostConditionCheckReport> checkReports = new ArrayList<>();
		for (PostconditionCheck thisCheck : postconditionChecks) {
				thisCheck.runCheck(build);
				checkReports.add(thisCheck.getReport());
		}
		return checkReports;
	}

	public PostconditionManager postconditionChecks(PostconditionCheck... postconditionCheckArray) {
		List<PostconditionCheck> postconditionChecks = new ArrayList<>();
		for (PostconditionCheck check : postconditionCheckArray) {
			postconditionChecks.add(check);
		}
		this.postconditionChecks = postconditionChecks;
		return this;
	}

	public void setPostconditionChecks(List<PostconditionCheck> postconditionChecks) {
		this.postconditionChecks = postconditionChecks;
	}
}
