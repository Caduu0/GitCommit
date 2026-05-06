package com.example.gitcommitmirror.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// Registra o ThreadPoolTaskScheduler como bean TaskScheduler.
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("commit-scheduler-");
        scheduler.setRemoveOnCancelPolicy(true); // libera a task da fila ao cancelar
        scheduler.initialize();
        return scheduler;
    }
}