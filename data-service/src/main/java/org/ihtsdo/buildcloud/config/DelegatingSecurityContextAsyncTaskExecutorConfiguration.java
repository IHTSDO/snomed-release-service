package org.ihtsdo.buildcloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class DelegatingSecurityContextAsyncTaskExecutorConfiguration {

	@Bean
	public DelegatingSecurityContextAsyncTaskExecutor securityContextAsyncTaskExecutor(
			@Value("${delegate.security.context.async.task.executor.thread.pool.size}") final int threadPoolSize) {
		return new DelegatingSecurityContextAsyncTaskExecutor(getAsyncTaskExecutor(threadPoolSize));
	}

	@Bean
	public AsyncTaskExecutor getAsyncTaskExecutor(final int threadPoolSize) {
		final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(threadPoolSize);
		return threadPoolTaskExecutor;
	}
}
