package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.Package;

/**
 * Checks that the first time release flag is compatible with the Previous Published Package value
 * @author Peter
 *
 */
public class CheckFirstReleaseFlag extends PreconditionCheck {
	
	public void runCheck(Package pkg, Execution execution) {
		//Perhaps another example where we should be driving off the execution's copy, not the build?

		if (pkg.isFirstTimeRelease() && pkg.getPreviousPublishedPackage() != null) {
			fail ("Cannot have a previous published package specified for a first time release");
		} else if ( !pkg.isFirstTimeRelease() && pkg.getPreviousPublishedPackage() == null ) {
			fail ("Subsequent releases must have a previous published package specified");
		} else {
			pass();
		}
	}
}
