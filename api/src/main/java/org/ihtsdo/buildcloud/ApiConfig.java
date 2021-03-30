package org.ihtsdo.buildcloud;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@PropertySources({
		@PropertySource(value = "classpath:api-application.properties"),
		@PropertySource(value = "file:${data.service.config.location}/api-application.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
public class ApiConfig {

	@Configuration
	@EnableWebSecurity
	@Order(1)
	public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Bean
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth, @Value("${srs.basicAuth.username}") final String username,
				@Value("${srs.basicAuth.password}") final String password) throws Exception {
			auth.inMemoryAuthentication().withUser(username).password(password).authorities("ROLE_USER");
		}

		@Bean
		public RequestHeaderAuthenticationDecorator authenticationDecorator() {
			return new RequestHeaderAuthenticationDecorator();
		}

		@Override
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
							"/**/webjars/**")
					.permitAll();
			http.csrf().disable();
			http.addFilterAfter(authenticationDecorator(), BasicAuthenticationFilter.class);
		}

		@Bean
		public Http403ForbiddenEntryPoint http403ForbiddenEntryPoint() {
			return new Http403ForbiddenEntryPoint();
		}
	}
}
