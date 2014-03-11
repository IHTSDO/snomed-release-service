package org.ihtsdo.buildcloud.builder;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderRunner {

	private final IQueue<String> queue;
	private final MavenBuilder mavenBuilder;

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenBuilderImpl.class);

	private BuilderRunner() throws InterruptedException {
		mavenBuilder = new MavenBuilderImpl();
		HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
		queue = hazelcastInstance.getQueue("org.ihtsdo.buildcloud.build.queue");
	}

	private void run() throws InterruptedException {
		while (true) {
			String executionS3Path = queue.take();
			LOGGER.info("Taking item '{}'", executionS3Path);

			// todo: Download build scripts.

			// todo: run build
//			mavenBuilder.exec();

			// todo: upload output
		}
	}

	public static void main(String[] args) throws InterruptedException {
		new BuilderRunner().run();
	}

}
