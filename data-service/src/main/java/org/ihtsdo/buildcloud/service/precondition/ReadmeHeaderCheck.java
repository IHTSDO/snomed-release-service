package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Package;

public class ReadmeHeaderCheck extends PreconditionCheck {

	@Override
	public void runCheck(Package pkg) {

		//Do we have a readme header?
		if ( pkg.getReadmeHeader() != null && pkg.getReadmeHeader().length() > 0 ) {
			pass();
		} else {
			fail("No Readme Header detected.");
		}
	}

}
