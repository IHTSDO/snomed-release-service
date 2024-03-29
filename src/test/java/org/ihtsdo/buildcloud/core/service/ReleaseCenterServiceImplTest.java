package org.ihtsdo.buildcloud.core.service;

import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.entity.helper.TestEntityGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class ReleaseCenterServiceImplTest extends TestEntityGenerator {

	@Test
	public void testCreate() throws Exception{
		final List<ReleaseCenter> releaseCentersExpected = new ArrayList<>();
		releaseCentersExpected.add(new ReleaseCenter("my test releaseCenter name1",
				"some short name1", "code system1"));
		final ReleaseCenterService rcs = Mockito.mock(ReleaseCenterService.class);
		assertNotNull(rcs);
		Mockito.when(rcs.findAll()).thenReturn(releaseCentersExpected);
		List<ReleaseCenter> releaseCenters = rcs.findAll();
		int before = releaseCenters.size();
		//LOGGER.warn("Found " + before + " release centers");
		assertTrue(before > 0);  //Check our test data is in there.
		final ReleaseCenter releaseCenter =
				new ReleaseCenter("my test releaseCenter name2", "some short name2", "code system2");
		Mockito.when(rcs.create("my test releaseCenter name2", "some short name2", "code system2", null))
				.thenReturn(releaseCenter);
		releaseCentersExpected.add(releaseCenter);
		Mockito.when(rcs.findAll()).thenReturn(releaseCentersExpected);
		int after = rcs.findAll().size();
		//LOGGER.warn("After create, found " + after + " release centers");
		assertEquals(before + 1, after);
		
		//TODO Could add further test to ensure that the new item was created at the correct point in the hierarchy
	}

}
