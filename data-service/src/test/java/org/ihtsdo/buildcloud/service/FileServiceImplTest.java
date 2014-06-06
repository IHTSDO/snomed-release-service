package org.ihtsdo.buildcloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext.xml"})
@Transactional
public class FileServiceImplTest {

	@Autowired
	private FileService fileService;

	@Test
	public void test() throws IOException {
		OutputStream outputStream = fileService.getExecutionOutputFileOutputStream("out.txt");
		Assert.assertNotNull(outputStream);
		outputStream.close();
	}

}
