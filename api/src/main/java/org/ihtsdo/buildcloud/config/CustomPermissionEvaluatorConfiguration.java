package org.ihtsdo.buildcloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomPermissionEvaluatorConfiguration {

	@Bean
	public MethodSecurityConfig customPermissionEvaluator() {
		return new MethodSecurityConfig();
	}
}
