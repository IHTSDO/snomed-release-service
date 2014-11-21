package org.ihtsdo.buildcloud.entity.helper;

import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.buildcloud.entity.Execution;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;

import java.util.GregorianCalendar;

public class TestEntityFactory extends TestEntityGenerator {

	public Build createBuild(String releaseCenterName, String releaseCenterShortName, String buildName) {
		ReleaseCenter releaseCenter = new ReleaseCenter(releaseCenterName, releaseCenterShortName);
		Build build = new Build(1L, buildName);
		releaseCenter.addBuild(build);
		return build;
	}
	
	public Build createBuild(){
		ReleaseCenter releaseCenter = new ReleaseCenter(releaseCenterNames[0], releaseCenterShortNames[0]);
		Build build = new Build(1L, buildNames[0]);
		releaseCenter.addBuild(build);
		return build;
	}

	public Execution createExecution() {
		Build build = createBuild();
		return new Execution(new GregorianCalendar(2013, 2, 5, 16, 30, 00).getTime(), build);
	}

}
