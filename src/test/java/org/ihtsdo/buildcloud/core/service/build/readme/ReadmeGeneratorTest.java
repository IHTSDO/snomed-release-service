package org.ihtsdo.buildcloud.core.service.build.readme;

import org.ihtsdo.buildcloud.test.StreamTestUtils;
import org.ihtsdo.buildcloud.core.manifest.ListingType;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ReadmeGeneratorTest {

	private ReadmeGenerator readmeGenerator;
	private ListingType manifestListing;
	private String readmeHeader;

	@BeforeEach
	public void setUp() throws Exception {
		readmeGenerator = new ReadmeGenerator();

		JAXBContext jc = JAXBContext.newInstance("org.ihtsdo.buildcloud.core.manifest");
		Unmarshaller um = jc.createUnmarshaller();
		InputStream resourceAsStream = getClass().getResourceAsStream("readme-test-manifest.xml");
		assertNotNull(resourceAsStream,"Test manifest stream should not be null");
		manifestListing = um.unmarshal(new StreamSource(resourceAsStream), ListingType.class).getValue();
		readmeHeader = StreamUtils.copyToString(getClass().getResourceAsStream("readme-header.txt"), RF2Constants.UTF_8);
	}

	@Test
	public void testGenerate() throws Exception {
		assertNotNull(manifestListing, "ListingType object should not be null");
		assertNotNull(manifestListing.getFolder(), "Manifest root folder should not be null");
		ByteArrayOutputStreamRecordClose readmeOutputStream = new ByteArrayOutputStreamRecordClose();
		assertFalse(readmeOutputStream.closeCalled);

		readmeGenerator.generate(readmeHeader, "2014", manifestListing, readmeOutputStream);

		assertTrue(readmeOutputStream.closeCalled, "OutputStream should be closed");
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
