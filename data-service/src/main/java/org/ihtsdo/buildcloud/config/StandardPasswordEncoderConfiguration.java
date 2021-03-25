package org.ihtsdo.buildcloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

@Configuration
public class StandardPasswordEncoderConfiguration {

	@Bean
	public StandardPasswordEncoder standardPasswordEncoder(@Value("${encryption.salt}") final String salt) {
		return new StandardPasswordEncoder(salt);
	}
}
