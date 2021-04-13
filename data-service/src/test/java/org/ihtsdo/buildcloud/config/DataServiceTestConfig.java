package org.ihtsdo.buildcloud.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration
@ComponentScan("org.ihtsdo.buildcloud.*")
@TestConfiguration
public class DataServiceTestConfig extends DataServiceConfig {
}
