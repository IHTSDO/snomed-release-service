package org.ihtsdo.telemetry.config;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelemetryConfig {

	@Bean
	public TransferManager transferManager(@Value("${aws.key}") final String accessKey,
			@Value("${aws.privateKey}") final String secretKey) {
		return new TransferManager(new BasicAWSCredentials(accessKey, secretKey));
	}
}
