package org.ihtsdo.buildcloud;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.usage.SystemUsage;

public class JMSBrokerManager {

	public JMSBrokerManager() throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616");
		// Unable to start up service in Dev right - stalls at this :
		// 20150227_17:19:21 ERROR BrokerService - Temporary Store limit is 51200 mb, whilst the temporary data directory:
		// /mnt/pd1/var/opt/snomed-release-service-api/run/activemq-data/localhost/tmp_storage only has 38992 mb of usable space - resetting
		// to maximum available 38992 mb.
		broker.setPersistent(false);
		SystemUsage systemUsage = broker.getSystemUsage();
		systemUsage.getStoreUsage().setLimit(1024 * 1024 * 8);
		systemUsage.getTempUsage().setLimit(1024 * 1024 * 8);
		broker.start();
	}
}
