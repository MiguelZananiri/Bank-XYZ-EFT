package com.duoc.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor(
            @Value("${app.poolSize}") int poolSize) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("batch-worker-");

        // Permite que los threads terminen cuando quedan inactivos.
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(1);

        executor.initialize();

        return executor;
    }
}