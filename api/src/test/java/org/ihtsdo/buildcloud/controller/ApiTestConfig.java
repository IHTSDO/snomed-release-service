package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.config.DataServiceConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "org.ihtsdo.buildcloud")
@TestConfiguration
public class ApiTestConfig extends DataServiceConfig {
}
