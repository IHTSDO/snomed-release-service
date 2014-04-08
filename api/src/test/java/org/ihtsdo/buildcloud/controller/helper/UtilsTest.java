package org.ihtsdo.buildcloud.controller.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.http.Part;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {
	
	public static String TEST_FILENAME = "SnomedCT_Release_INT_20140131.xml";
	
	public static String TEST_CONTENT = "form-data; name=\"file\"; filename=\"" + TEST_FILENAME + "\"";

	@Test
	public void testGetFilename() {
		String fileName = Utils.getFilename(new MockPart(), "otherFileName");
		Assert.assertEquals(TEST_FILENAME, fileName);
	}
	
	
	public class MockPart implements Part {
		
		public long getSize() { return 0; }

		@Override
		public void delete() throws IOException {}

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public String getHeader(String arg0) {
			return TEST_CONTENT;
		}

		@Override
		public Collection<String> getHeaderNames() {
			return null;
		}

		@Override
		public Collection<String> getHeaders(String arg0) {
			return null;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void write(String arg0) throws IOException {}
		
	}

}
