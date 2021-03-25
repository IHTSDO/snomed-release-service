package org.ihtsdo.buildcloud.config;

import com.amazonaws.auth.BasicAWSCredentials;
import org.ihtsdo.otf.dao.s3.OfflineS3ClientImpl;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.dao.s3.S3ClientFactory;
import org.ihtsdo.otf.dao.s3.S3ClientImpl;
import org.ihtsdo.otf.dao.s3.helper.S3ClientHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;

@Configuration
public class S3ClientFactoryConfiguration {

	private S3ClientFactory s3ClientFactory;

	@Bean
	public S3ClientFactory s3ClientFactory(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String privateKey,
			@Value("${s3.offline.directory}") final String directory) throws IOException {
		s3ClientFactory = new S3ClientFactory();
		s3ClientFactory.setOnlineImplementation(getOnlineImplementation(accessKey, privateKey));
		s3ClientFactory.setOfflineImplementation(getOfflineImplementation(directory));
		return s3ClientFactory;
	}

	private S3Client getOnlineImplementation(final String accessKey, final String privateKey) {
		return new S3ClientImpl(new BasicAWSCredentials(accessKey, privateKey));
	}

	private S3Client getOfflineImplementation(final String directory) throws IOException {
		return new OfflineS3ClientImpl(directory);
	}

	@Bean
	@DependsOn("s3ClientFactory")
	public S3Client getClient(@Value("${offlineMode}") final boolean offlineMode) {
		return s3ClientFactory.getClient(offlineMode);
	}

	@Bean
	@DependsOn("s3ClientFactory")
	public S3ClientHelper s3ClientHelper(@Value("${offlineMode}") final boolean offlineMode) {
		return new S3ClientHelper(getClient(offlineMode));
	}
}
