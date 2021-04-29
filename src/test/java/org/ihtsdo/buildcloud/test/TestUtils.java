package org.ihtsdo.buildcloud.test;

import org.ihtsdo.buildcloud.dao.helper.BuildS3PathHelper;
import org.ihtsdo.buildcloud.entity.Build;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class TestUtils {

	@Autowired
	private BuildS3PathHelper pathHelper;

	@Autowired
	private S3Client s3Client;

	@Value("${srs.build.bucketName}")
	private String buildBucketName;

	/**
	 * Deletes the entire structure under an build.  To ONLY BE USED with unit tests,
	 * as in the normal course of events we expect build files to be immutable
	 *
	 * @param build
	 */
	public void scrubBuild(Build build) {
		String directoryPath = pathHelper.getBuildPath(build).toString();
		try {
			s3Client.deleteObject(buildBucketName, directoryPath);
		} catch (Exception e) {
		}  //That's fine if the thing to delete doesn't exist.
	}

	public static String readStream(InputStream inputStream) throws IOException {
		StringBuilder builder = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				builder.append(line);
			}
		}
		return builder.toString();
	}
}
