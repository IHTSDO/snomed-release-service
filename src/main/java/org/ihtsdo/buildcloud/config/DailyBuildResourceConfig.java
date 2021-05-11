package org.ihtsdo.buildcloud.config;

import org.ihtsdo.otf.resourcemanager.ResourceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="dailybuild.storage")
public class DailyBuildResourceConfig extends ResourceConfiguration {
	
}
