package org.ihtsdo.buildcloud.core.service.monitor;

import org.ihtsdo.buildcloud.core.entity.Build;

public interface MonitorService {
	void startMonitorBuild(Build build, String username);
}
