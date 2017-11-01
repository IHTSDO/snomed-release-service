import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.ihtsdo.buildcloud.controller.AbstractControllerTest;
import org.ihtsdo.buildcloud.dao.BuildDAO;
import org.ihtsdo.buildcloud.entity.ReleaseCenter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TestHarness extends AbstractControllerTest{
	@Autowired
	BuildDAO buildDAO;
	@Test
	public void testGetPublishedFileArchiveEntry() throws IOException {
		ReleaseCenter releaseCenter = new ReleaseCenter("Swedish", "SE");
		String targetFileName = "der2_Refset_UrvalDeltagandetyperHälso-OchSjukvårdSimpleRefsetFull_SE1000052_20171130.txt";
		String previousPublishedPackage = "SnomedCT_SE_Production_20170531T120000.zip";
		InputStream inputStream = buildDAO.getPublishedFileArchiveEntry(releaseCenter, targetFileName, previousPublishedPackage);
		Assert.assertNotNull(inputStream);
	}

}
