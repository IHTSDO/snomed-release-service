package org.ihtsdo.buildcloud.service.precondition;

import org.ihtsdo.buildcloud.entity.Package;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CheckFirstReleaseFlagTest extends PreconditionCheckTest {
	
	private static final String PPP = "SomeValue.zip";

	@Before
	public void setup() {
		super.setup();
		manager = new PreconditionManager().preconditionChecks(new CheckFirstReleaseFlag());
	}

	@Test
	public void testFirstCorrect() throws InstantiationException, IllegalAccessException {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(true);
			p.setPreviousPublishedPackage(null);
		}
		
		String actualResult = runPreConditionCheck(CheckFirstReleaseFlag.class);
		Assert.assertEquals( PreconditionCheck.State.PASS.toString(), actualResult);		

	}
	
	@Test
	public void testFirstIncorrect() throws InstantiationException, IllegalAccessException {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(true);
			p.setPreviousPublishedPackage(PPP);
		}
		
		String actualResult = runPreConditionCheck(CheckFirstReleaseFlag.class);
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);		
	}
	
	@Test
	public void testSubsequentCorrect() throws InstantiationException, IllegalAccessException {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(false);
			p.setPreviousPublishedPackage(PPP);
		}
		
		String actualResult = runPreConditionCheck(CheckFirstReleaseFlag.class);
		Assert.assertEquals( PreconditionCheck.State.PASS.toString(), actualResult);	
	}
	
	@Test
	public void testSubsequentIncorrect() throws InstantiationException, IllegalAccessException {

		for (Package p : build.getPackages()) {
			p.setFirstTimeRelease(false);
			p.setPreviousPublishedPackage(null);
		}
		
		String actualResult = runPreConditionCheck(CheckFirstReleaseFlag.class);
		Assert.assertEquals( PreconditionCheck.State.FAIL.toString(), actualResult);	
	}
	


}
