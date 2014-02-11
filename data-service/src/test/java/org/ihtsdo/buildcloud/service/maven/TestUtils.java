package org.ihtsdo.buildcloud.service.maven;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	/**
	 * 
	 * @param thisDir - the directory to be scanned/counted
	 * @return the number of things in this directory structure (recursively) - both files and directories
	 * @throws FileNotFoundException
	 */
	public static int itemCount (File thisDir) throws FileNotFoundException {
		
		int result = 0;

		if (!thisDir.isDirectory()) {
			throw new FileNotFoundException (thisDir.getName() + " is not a valid Directory.");
		} 
		
		LOGGER.debug ("Examinining directory: " + thisDir.getName());
		File[] files = thisDir.listFiles();

		for (File thisFile : files){
			if(thisFile.isFile()) {
				result++;
			} else if (thisFile.isDirectory()) {
				result++;
				result += itemCount (thisFile);
			} else {
				throw new FileNotFoundException ("Unexpected thing found: " +  thisFile.getPath());
			}
		}
		return result;
	}

}
