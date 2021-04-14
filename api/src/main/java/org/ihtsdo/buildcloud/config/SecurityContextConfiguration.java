package org.ihtsdo.buildcloud.config;

import org.ihtsdo.sso.integration.RequestHeaderAuthenticationDecorator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityContextConfiguration extends WebSecurityConfigurerAdapter {

	@Bean
	public UserDetailsService userDetailsService(@Value("${srs.basicAuth.username}") final String username,
											 @Value("${srs.basicAuth.password}") final String password, @Value("ROLE_USER") final String authority) {
		return new InMemoryUserDetailsManager(Collections.singleton(new User(username, password,
				Collections.singleton(new SimpleGrantedAuthority(authority)))));
	}

	@Bean
	public RequestHeaderAuthenticationDecorator authenticationDecorator() {
		return new RequestHeaderAuthenticationDecorator();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/**/api-doc.html",
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