package org.ihtsdo.buildcloud.service.build.readme;

import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.buildcloud.manifest.ListingType;
import org.ihtsdo.buildcloud.service.build.RF2Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StreamUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadmeGeneratorTest {

	private ReadmeGenerator readmeGenerator;
	private ListingType manifestListing;
	private String readmeHeader;

	@Before
	public void setUp() throws Exception {
		readmeGenerator = new ReadmeGenerator();

		JAXBContext jc = JAXBContext.newInstance("org.ihtsdo.buildcloud.manifest");
		Unmarshaller um = jc.createUnmarshaller();
		InputStream resourceAsStream = getClass().getResourceAsStream("readme-test-manifest.xml");
		Assert.assertNotNull("Test manifest stream should not be null", resourceAsStream);
		manifestListing = um.unmarshal(new StreamSource(resourceAsStream), ListingType.class).getValue();
		readmeHeader = StreamUtils.copyToString(getClass().getResourceAsStream("readme-header.txt"), RF2Constants.UTF_8);
	}

	@Test
	public void testGenerate() throws Exception {
		Assert.assertNotNull("ListingType object should not be null", manifestListing);
		Assert.assertNotNull("Manifest root folder should not be null", manifestListing.getFolder());
		ByteArrayOutputStreamRecordClose readmeOutputStream = new ByteArrayOutputStreamRecordClose();
		Assert.assertFalse(readmeOutputStream.closeCalled);

		readmeGenerator.generate(readmeHeader, "2014", manifestListing, readmeOutputStream);

		Assert.assertTrue("OutputStream should be closed", readmeOutputStream.closeCalled);
		StreamTestUtils.assertStreamsEqualLineByLine(getClass().getResourceAsStream("expected-readme.txt"), new ByteArrayInputStream(readmeOutputStream.toByteArray()));
	}

	private static class ByteArrayOutputStreamRecordClose extends ByteArrayOutputStream {

		private boolean closeCalled;

		@Override
		public void close() throws IOException {
			closeCalled = true;
			super.close();
		}
	}

}
