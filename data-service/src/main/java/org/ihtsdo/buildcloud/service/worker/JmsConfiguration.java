package org.ihtsdo.buildcloud.service.worker;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.Queue;

@Configuration
public class JmsConfiguration {

	@Bean
	public Queue queue() {
		return new ActiveMQQueue("srs.jms.job.queue");
	}
}
