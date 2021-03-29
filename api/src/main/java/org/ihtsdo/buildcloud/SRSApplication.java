package org.ihtsdo.buildcloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import springfox.documentation.PathProvider;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

import static com.google.common.base.Predicates.not;
import static springfox.documentation.builders.PathSelectors.regex;

@EnableSwagger2
@SpringBootApplication
@PropertySources({
		@PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "file:${data.service.config.location}/application.properties", ignoreResourceNotFound=true)})
@EnableConfigurationProperties
public class SRSApplication extends ApiConfig {

	@Autowired(required = false)
	private BuildProperties buildProperties;

	public static void main(String[] args) {
		SpringApplication.run(SRSApplication.class, args);
	}

	@Bean
	public Docket swagger() {
		return new Docket(DocumentationType.SWAGGER_2)
				.apiInfo(apiInfo())
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(not(regex("/error")))
				.build();
	}

	private ApiInfo apiInfo() {
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		return new ApiInfo("SRS API Docs",
				"This is a listing of available apis of SNOMED release service. For more technical details visit "
				+ "<a src='https://github.com/IHTSDO/snomed-release-service' > SNOMED Release Service </a> page @ github.com ",
				version,
				null,
				new Contact("SNOMED International", "https://github.com/IHTSDO/snomed-release-service", "info@ihtsdotools.org"),
				"Apache License, Version 2.0",
				"http://www.apache.org/licenses/LICENSE-2.0",
				Collections.emptyList());
	}
}
