package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.service.execution.RF2Constants;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Checks the following configuration is set correctly.
 * 1.The first time release
 * flag is compatible with the Previous Published Package value
 * 2.Check the effective time for a given build is set
 * 3.Copyright end date is set.
 * 4.Check previous release date is before current effective time if previous release is
 * configured.
 *
 */
public class ConfigurationCheck extends PreconditionCheck {

	private static final String NO_README_HEADER_DETECTED = "No Readme Header detected.";
	private static final String NO_COPYRIGHT_END_DATE = "The copyright end date is not set.";
	private static final String INVALID_RELEASE_DATE_FORMAT = "Expecting release date format in package file name to be yyyyMMdd but is %s";
	private static final String INVALID_PREVIOUS_PUBLISHED_RELEASE_DATE = "Previous release date %s in published package is not before current effective time %s";
	private static final String NO_EFFECTIVE_TIME = "Effective time is not specified in the build.";
	private static final String INVALID_SUBSEQUENT_RELEASE_CONFIG_ERROR_MSG = "Subsequent releases must have a previous published package specified.";
	private static final String INVALID_FIRST_TIME_REPLEASE_CONFIG_ERROR_MSG = "Cannot have a previous published package specified for a first time release.";

	@Override
	public final void runCheck(final Execution execution) {
		// Perhaps another example where we should be driving off the
		// execution's copy, not the build?
		List<String> errorList = new ArrayList<>();

		Build build = execution.getBuild();
		if (build.isFirstTimeRelease() && build.getPreviousPublishedPackage() != null) {
			errorList.add(INVALID_FIRST_TIME_REPLEASE_CONFIG_ERROR_MSG);
		} else if (!build.isFirstTimeRelease() && build.getPreviousPublishedPackage() == null) {
			errorList.add(INVALID_SUBSEQUENT_RELEASE_CONFIG_ERROR_MSG);
		}
		// effective time check
		Date effectiveTime = build.getEffectiveTime();
		if (effectiveTime == null) {
			errorList.add(NO_EFFECTIVE_TIME);
		}
		// check the published release date is in the past
		if (build.getPreviousPublishedPackage() != null && effectiveTime != null) {
			String[] tokens = build.getPreviousPublishedPackage().split(RF2Constants.FILE_NAME_SEPARATOR);
			if (tokens.length > 0) {
				String releaseDateStr = tokens[tokens.length - 1].replace(RF2Constants.ZIP_FILE_EXTENSION, "");
				try {
					Date preReleasedDate = RF2Constants.DATE_FORMAT.parse(releaseDateStr);
					if (!preReleasedDate.before(effectiveTime)) {
						errorList.add(String.format(INVALID_PREVIOUS_PUBLISHED_RELEASE_DATE, preReleasedDate, effectiveTime));
					}
				} catch (ParseException e) {
					errorList.add(String.format(INVALID_RELEASE_DATE_FORMAT, releaseDateStr));
				}
			}
		}
		String readmeEndDate = build.getReadmeEndDate();
		if (readmeEndDate == null || readmeEndDate.length() < 4) {
			errorList.add(NO_COPYRIGHT_END_DATE);
		}

		if (build.getReadmeHeader() == null || build.getReadmeHeader().isEmpty()) {
			errorList.add(NO_README_HEADER_DETECTED);
		}

		if (errorList.size() > 0) {
			StringBuilder msgBuilder = new StringBuilder();
			for (int i = 0; i < errorList.size(); i++) {
				if (i > 0) {
					msgBuilder.append(" ");
				}
				msgBuilder.append(errorList.get(i));
			}
			fail(msgBuilder.toString());
		} else {
			pass();
		}
	}
}
