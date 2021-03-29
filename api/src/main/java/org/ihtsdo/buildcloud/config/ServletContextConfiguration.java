package org.ihtsdo.buildcloud.config;

import org.ihtsdo.buildcloud.controller.helper.HypermediaGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class ServletContextConfiguration {

	@Bean
	@DependsOn(value = "objectMapper")
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter =
				new MappingJackson2HttpMessageConverter();
		mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper().getObject());
		return mappingJackson2HttpMessageConverter;
	}

	@Bean
	public Jackson2ObjectMapperFactoryBean objectMapper() {
		final Jackson2ObjectMapperFactoryBean jackson2ObjectMapperFactoryBean =
				new Jackson2ObjectMapperFactoryBean();
		jackson2ObjectMapperFactoryBean.setIndentOutput(true);
		return jackson2ObjectMapperFactoryBean;
	}

	@Bean
	public HypermediaGenerator hypermediaGenerator() {
		return new HypermediaGenerator();
	}

	@Bean
	public StandardServletMultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}
}
