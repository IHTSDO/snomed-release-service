package org.ihtsdo.buildcloud.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;

@TestConfiguration
@SpringBootApplication(exclude = {
		ContextInstanceDataAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		ActiveMQAutoConfiguration.class}
)
public class TestConfig extends Config {

}

