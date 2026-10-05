package com.duoc.demo.Config;

import java.io.IOException;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Listener.InteresesSkipListener;
import com.duoc.demo.Model.LegacyIntereses;
import com.duoc.demo.Model.ModernIntereses;
import com.duoc.demo.Partition.BankPartitioner;
import com.duoc.demo.Policy.BankSkipPolicy;
import com.duoc.demo.Processor.LegacyInteresesProcessor;
import com.duoc.demo.Reader.InteresesReader;
import com.duoc.demo.Writer.InteresesWriter;

import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.DuplicateKeyException;

@Configuration
public class InteresesConfig {

    @Bean
    @StepScope
    public InteresesReader interesesReader(
            @Value("#{stepExecutionContext['start']}") Integer start,
            @Value("#{stepExecutionContext['end']}") Integer end)
             throws IOException {
        return new InteresesReader(
            "data/intereses.csv",
            start,
            end);
    }

    @Bean
    @StepScope
    public LegacyInteresesProcessor interesesProcessor() {
        return new LegacyInteresesProcessor();
    }

    @Bean
    public InteresesWriter interesesWriter(JdbcTemplate jdbcTemplate) {
        return new InteresesWriter(jdbcTemplate);
    }

    @Bean
    Step stepLimpiarIntereses(
            JobRepository jobRepository,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder(
                "stepLimpiarIntereses",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    jdbcTemplate.update(
                            "DELETE FROM INTERESES_PROCESADOS");

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    Step stepIntereses(
            JobRepository jobRepository,
            InteresesReader reader,
            LegacyInteresesProcessor processor,
            InteresesWriter writer,
            BankSkipPolicy bankSkipPolicy,
            InteresesSkipListener interesesSkipListener) {

        return new StepBuilder("stepIntereses", jobRepository)
                .<LegacyIntereses, ModernIntereses>chunk(5)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retry(DuplicateKeyException.class)
                .retryLimit(2)
                .skipPolicy(bankSkipPolicy)
                .listener(interesesSkipListener)
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandlerIntereses(
        Step stepIntereses,
         TaskExecutor taskExecutor) {

        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(stepIntereses);
        handler.setTaskExecutor(taskExecutor);
        handler.setGridSize(5);
        return handler;
    }

    @Bean
    public Step partitionStepIntereses(
        JobRepository jobRepository,
        BankPartitioner bankPartitioner,
        @Qualifier("partitionHandlerIntereses")
        TaskExecutorPartitionHandler partitionHandler) {

        return new StepBuilder("partitionStepIntereses", jobRepository)
                .partitioner("stepIntereses", bankPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

}