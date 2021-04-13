package org.ihtsdo.buildcloud;

import org.apache.activemq.broker.BrokerService;
import org.springframework.stereotype.Service;

@Service
public class JMSBrokerManager {

	public JMSBrokerManager() throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616");
		broker.setBrokerName("TelemetryJMSBroker");
		broker.start();
	}
}
