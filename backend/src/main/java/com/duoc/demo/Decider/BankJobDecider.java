package com.duoc.demo.Decider;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class BankJobDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(
            JobExecution jobExecution,
            StepExecution stepExecution) {

        long errores = jobExecution.getStepExecutions()
                .stream()
                .filter(step -> step.getStepName().contains(":partition"))
                .mapToLong(StepExecution::getSkipCount)
                .sum();

        System.out.println(
                "DECIDER: registros omitidos = " + errores);

        if (errores == 0) {
            return new FlowExecutionStatus("SIN_ERRORES");
        }

        return new FlowExecutionStatus("CON_ERRORES");
    }
}