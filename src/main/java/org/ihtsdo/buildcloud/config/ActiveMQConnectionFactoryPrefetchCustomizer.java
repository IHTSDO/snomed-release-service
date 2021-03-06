package org.ihtsdo.buildcloud.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;

public class ActiveMQConnectionFactoryPrefetchCustomizer implements ActiveMQConnectionFactoryCustomizer {

	private final int queuePrefetch;

	ActiveMQConnectionFactoryPrefetchCustomizer(final int queuePrefetch) {
		this.queuePrefetch = queuePrefetch;
	}

	@Override
	public void customize(ActiveMQConnectionFactory factory) {
		ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
		prefetchPolicy.setQueuePrefetch(queuePrefetch);
		factory.setPrefetchPolicy(prefetchPolicy);
	}
}
