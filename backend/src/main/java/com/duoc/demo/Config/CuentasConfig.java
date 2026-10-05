package com.duoc.demo.Config;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.duoc.demo.Listener.CuentaSkipListener;
import com.duoc.demo.Model.LegacyCuentas;
import com.duoc.demo.Model.ModernCuentas;
import com.duoc.demo.Partition.BankPartitioner;
import com.duoc.demo.Policy.BankSkipPolicy;
import com.duoc.demo.Processor.LegacyCuentasProcessor;
import com.duoc.demo.Reader.CuentasReader;
import com.duoc.demo.Writer.CuentasWriter;

import org.springframework.dao.TransientDataAccessException;

@Configuration
public class CuentasConfig {

    @Bean
    @StepScope
    public CuentasReader cuentasReader(
            @Value("#{stepExecutionContext['start']}") Integer start,
            @Value("#{stepExecutionContext['end']}") Integer end)throws IOException {
        return new CuentasReader(
            "data/cuentas_anuales.csv",
            start,
            end);
    }

    @Bean
    @StepScope
    public LegacyCuentasProcessor cuentasProcessor() {
        return new LegacyCuentasProcessor();
    }

    @Bean
    public CuentasWriter cuentasWriter(JdbcTemplate jdbcTemplate) {
        return new CuentasWriter(jdbcTemplate);
    }

    @Bean
    Step stepCuentas(
            JobRepository jobRepository,
            CuentasReader reader,
            LegacyCuentasProcessor processor,
            CuentasWriter writer,
            BankSkipPolicy bankSkipPolicy,
            CuentaSkipListener cuentaSkipListener) {

        return new StepBuilder("stepCuentas", jobRepository)
                .<LegacyCuentas, ModernCuentas>chunk(5)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(2)
                .skipPolicy(bankSkipPolicy)
                .listener(cuentaSkipListener)
                .build();
    }

    @Bean
    Step stepResumenAnual(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = """
                MERGE INTO ESTADOS_CUENTA_ANUALES destino
                USING (
                    SELECT
                        CUENTA_ID,
                        EXTRACT(YEAR FROM FECHA) AS ANIO,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'deposito'
                                THEN MONTO
                                ELSE 0
                            END
                        ) AS TOTAL_DEPOSITOS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'retiro'
                                THEN ABS(MONTO)
                                ELSE 0
                            END
                        ) AS TOTAL_RETIROS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                AND TRANSACCION = 'compra'
                                THEN ABS(MONTO)
                                ELSE 0
                            END
                        ) AS TOTAL_COMPRAS,

                        COUNT(*) AS CANTIDAD_MOVIMIENTOS,

                        SUM(
                            CASE
                                WHEN ESTADO = 'VALIDA'
                                THEN MONTO
                                ELSE 0
                            END
                        ) AS SALDO_ANUAL,

                        SUM(
                            CASE
                                WHEN ESTADO = 'ANOMALIA'
                                THEN 1
                                ELSE 0
                            END
                        ) AS CANTIDAD_ANOMALIAS

                    FROM MOVIMIENTOS_ANUALES_PROCESADOS
                    GROUP BY
                        CUENTA_ID,
                        EXTRACT(YEAR FROM FECHA)
                ) origen

                ON (
                    destino.CUENTA_ID = origen.CUENTA_ID
                    AND destino.ANIO = origen.ANIO
                )

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.TOTAL_DEPOSITOS = origen.TOTAL_DEPOSITOS,
                        destino.TOTAL_RETIROS = origen.TOTAL_RETIROS,
                        destino.TOTAL_COMPRAS = origen.TOTAL_COMPRAS,
                        destino.CANTIDAD_MOVIMIENTOS = origen.CANTIDAD_MOVIMIENTOS,
                        destino.SALDO_ANUAL = origen.SALDO_ANUAL,
                        destino.CANTIDAD_ANOMALIAS = origen.CANTIDAD_ANOMALIAS,
                        destino.FECHA_GENERACION = SYSTIMESTAMP

                WHEN NOT MATCHED THEN
                    INSERT (
                        CUENTA_ID,
                        ANIO,
                        TOTAL_DEPOSITOS,
                        TOTAL_RETIROS,
                        TOTAL_COMPRAS,
                        CANTIDAD_MOVIMIENTOS,
                        SALDO_ANUAL,
                        CANTIDAD_ANOMALIAS
                    )
                    VALUES (
                        origen.CUENTA_ID,
                        origen.ANIO,
                        origen.TOTAL_DEPOSITOS,
                        origen.TOTAL_RETIROS,
                        origen.TOTAL_COMPRAS,
                        origen.CANTIDAD_MOVIMIENTOS,
                        origen.SALDO_ANUAL,
                        origen.CANTIDAD_ANOMALIAS
                    )
                """;

        return new StepBuilder("stepResumenAnual", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(sql);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Step stepLimpiarMovimientos(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new StepBuilder("stepLimpiarMovimientos", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update(
                            "DELETE FROM MOVIMIENTOS_ANUALES_PROCESADOS");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandlerCuentas(
            Step stepCuentas,
            TaskExecutor taskExecutor
    ) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(stepCuentas);
        handler.setTaskExecutor(taskExecutor);
        handler.setGridSize(5);
        return handler;
    }

    @Bean
    public Step partitionStepCuentas(
            JobRepository jobRepository,
            BankPartitioner bankPartitioner,
            @Qualifier("partitionHandlerCuentas") 
            TaskExecutorPartitionHandler partitionHandler
    ) {
        return new StepBuilder("partitionStepCuentas", jobRepository)
                .partitioner("stepCuentas", bankPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }
}