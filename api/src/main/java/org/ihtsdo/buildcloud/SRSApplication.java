package org.ihtsdo.buildcloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
public class SRSApplication extends ApiConfig {

	@Autowired(required = false)
	private BuildProperties buildProperties;

	public static void main(String[] args) {
		SpringApplication.run(SRSApplication.class, args);
	}

//	@Bean
//	public Docket swagger() {
//		return new Docket(DocumentationType.SWAGGER_2)
//				.apiInfo(apiInfo())
//				.select()
//				.apis(RequestHandlerSelectors.any())
//				.paths(not(regex("/error")))
//				.build();
//	}

//	private ApiInfo apiInfo() {
//		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
//		return new ApiInfo("SRS API Docs",
//				"This is a listing of available apis of SNOMED release service. For more technical details visit "
//				+ "<a src='https://github.com/IHTSDO/snomed-release-service' > SNOMED Release Service </a> page @ github.com ",
//				version,
//				null,
//				new Contact("SNOMED International", "https://github.com/IHTSDO/snomed-release-service", "info@ihtsdotools.org"),
//				"Apache License, Version 2.0",
//				"http://www.apache.org/licenses/LICENSE-2.0",
//				Collections.emptyList());
//	}
}
