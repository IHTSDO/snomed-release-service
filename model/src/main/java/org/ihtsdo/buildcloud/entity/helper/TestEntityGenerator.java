package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

public class TestEntityGenerator {

	public static final String [] releaseCenterNames = {"International Release Center"};

	public static final String [] releaseCenterShortNames = {"International"};

	public static final String [] buildNames =
			{"SNOMED CT Release",
					"NLM Example Refset",
					"Medical Devices Technical Preview",
					"GP/FP Refset Technical Preview",
					"LOINC Expressions Technical Preview",
					"ICPC2 Map Technical Preview",
					"Spanish Release"};

	protected ReleaseCenter createTestReleaseCenter(String fullName, String shortName) {
		return new ReleaseCenter(fullName, shortName);
	}

	protected ReleaseCenter createTestReleaseCenterWithBuilds(String fullName, String shortName) {
		ReleaseCenter releaseCenter = createTestReleaseCenter(fullName, shortName);
		addBuildsToReleaseCenter(releaseCenter);
		return releaseCenter;
	}

	protected void addBuildsToReleaseCenter(ReleaseCenter releaseCenter) {
		for (String buildName : buildNames) {
			releaseCenter.addBuild(new Build(buildName));
		}
	}
	


}
