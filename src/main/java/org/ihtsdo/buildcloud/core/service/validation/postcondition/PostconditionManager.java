package org.ihtsdo.buildcloud.core.service.validation.postcondition;

import org.ihtsdo.buildcloud.core.entity.Build;
import org.ihtsdo.buildcloud.core.entity.PostConditionCheckReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PostconditionManager {

	@Autowired
	private List<PostconditionCheck> postconditionChecks;

	public List<PostConditionCheckReport> runPostconditionChecks(final Build build) {
		List<PostConditionCheckReport> checkReports = new ArrayList<>();
		for (PostconditionCheck thisCheck : postconditionChecks) {
				if (thisCheck instanceof TermServerClassificationResultsOutputCheck && !build.getConfiguration().isClassifyOutputFiles()) {
					continue;
				}

				thisCheck.runCheck(build);
				checkReports.add(thisCheck.getReport());
		}
		return checkReports;
	}

	public PostconditionManager postconditionChecks(PostconditionCheck... postconditionCheckArray) {
		List<PostconditionCheck> postconditionChecks = new ArrayList<>();
		Collections.addAll(postconditionChecks, postconditionCheckArray);
		this.postconditionChecks = postconditionChecks;
		return this;
	}

	public void setPostconditionChecks(List<PostconditionCheck> postconditionChecks) {
		this.postconditionChecks = postconditionChecks;
	}
}
