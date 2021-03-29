package org.ihtsdo.buildcloud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityExpressionHandlerConfiguration extends GlobalMethodSecurityConfiguration {

	@Bean
	public DefaultMethodSecurityExpressionHandler expressionHandler(
			@Autowired final CustomPermissionEvaluatorConfiguration customPermissionEvaluatorConfiguration) {
		final DefaultMethodSecurityExpressionHandler defaultMethodSecurityExpressionHandler =
				new DefaultMethodSecurityExpressionHandler();
		defaultMethodSecurityExpressionHandler.setPermissionEvaluator(
				customPermissionEvaluatorConfiguration.customPermissionEvaluator());
		return defaultMethodSecurityExpressionHandler;
	}
}
