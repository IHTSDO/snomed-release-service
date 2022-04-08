package org.ihtsdo.buildcloud.config;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityContextConfiguration extends WebSecurityConfigurerAdapter {

	@Value("${srs.basicAuth.username}")
	private String username;

	@Value("${srs.basicAuth.password}")
	private String password;

	/*
	This configuration is required for basic authentication to work with this version of Spring Security (3.2.5.RELEASE).
	In later versions default user credentials are read from the following properties in the application.properties file:
		spring.security.user.name
		spring.security.user.password
		spring.security.user.roles
	This functionality is implemented in class InitializeUserDetailsBeanManagerConfigurer.java since 4.1.x
    */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
				.withUser(username)
				.password(password)
				.roles("USER");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.authorizeRequests()
				.antMatchers("/snomed-release-service-websocket/**/*",
						"/swagger-ui.html",
						"/swagger-resources/**",
						"/v2/api-docs",
						"/webjars/springfox-swagger-ui/**").permitAll()
				.anyRequest().authenticated()
				.and().httpBasic();
		http.csrf().disable();
		http.addFilterAfter(new RequestHeaderAuthenticationDecorator(), BasicAuthenticationFilter.class);
	}

	// Does not seem to be used anywhere
	@Bean
	public Http403ForbiddenEntryPoint http403ForbiddenEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}
}