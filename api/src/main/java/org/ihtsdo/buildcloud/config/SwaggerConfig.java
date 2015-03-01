package org.ihtsdo.buildcloud.config;

import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.paths.RelativeSwaggerPathProvider;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;

/**
*Class to support configuration of swagger based API documentation
*/
@Configuration
@EnableSwagger
public class SwaggerConfig {
	
	private SpringSwaggerConfig swaggerConfig;
	private RelativeSwaggerPathProvider pathProvider;

	@Autowired
	public void setSpringSwaggerConfig(SpringSwaggerConfig swaggerConfig) {
		
		this.swaggerConfig = swaggerConfig;
	}
   

	@Bean
	public SwaggerSpringMvcPlugin apiImplementation(){
		
		return new SwaggerSpringMvcPlugin(this.swaggerConfig)
      		 .apiInfo(apiInfo())
      		 .pathProvider(this.pathProvider);

	}
   
	private ApiInfo apiInfo() {
		
		return new ApiInfo(
				"SRS API Docs",
				"This is a listing of available apis of SNOMED release service. For more technical details visit "
				+ "<a src='https://github.com/IHTSDO/snomed-release-service' > SNOMED Release Service </a> page @ github.com ",
				"https://github.com/IHTSDO/snomed-release-service",
				"info@ihtsdotools.org",
				"Apache License, Version 2.0",
				"http://www.apache.org/licenses/LICENSE-2.0" 
				);
	}
	
	
	@Bean
	public RelativeSwaggerPathProvider setPathProvider(RelativeSwaggerPathProvider pathProvider) {
		
		this.pathProvider = pathProvider;
		this.pathProvider.setApiResourcePrefix("v1");
		
		return this.pathProvider;
		
	}
	
    
}
