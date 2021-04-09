package org.ihtsdo.buildcloud;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@Configuration
@PropertySources({
		@PropertySource(value = "classpath:api-application.properties"),
		@PropertySource(value = "file:${data.service.config.location}/application.properties", ignoreResourceNotFound=true)})
@EnableSwagger2
public class ApiConfig {

	@Configuration
	@EnableWebSecurity
	@Order(1)
	public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					.antMatchers("/swagger-ui.html",
							"/swagger-resources/**",
							"/v2/api-docs",
							"/v2/**",
							"/webjars/springfox-swagger-ui/**",
							"/**/api-doc.html",
							"/**/api-docs/**",
							"/**/static/**",
							"/**/webjars/**").permitAll()
					.anyRequest().authenticated()
					.and().httpBasic();
			http.csrf().disable();
			http.addFilterAfter(new RequestHeaderAuthenticationDecorator(), BasicAuthenticationFilter.class);
		}
	}
}
