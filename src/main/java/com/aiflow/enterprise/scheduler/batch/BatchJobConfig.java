package com.aiflow.enterprise.scheduler.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ReportGenerationTasklet reportGenerationTasklet;
    private final WorkflowCleanupTasklet workflowCleanupTasklet;
    private final BatchJobCompletionListener completionListener;

    public BatchJobConfig(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           ReportGenerationTasklet reportGenerationTasklet,
                           WorkflowCleanupTasklet workflowCleanupTasklet,
                           BatchJobCompletionListener completionListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reportGenerationTasklet = reportGenerationTasklet;
        this.workflowCleanupTasklet = workflowCleanupTasklet;
        this.completionListener = completionListener;
    }

    @Bean
    public Step reportGenerationStep() {
        return new StepBuilder("reportGenerationStep", jobRepository)
                .tasklet(reportGenerationTasklet, transactionManager)
                .allowStartIfComplete(true)
                .listener(new org.springframework.batch.core.listener.StepExecutionListenerSupport() {
                    @Override
                    public org.springframework.batch.core.ExitStatus afterStep(
                            org.springframework.batch.core.StepExecution stepExecution) {
                        if (stepExecution.getFailureExceptions() != null
                                && !stepExecution.getFailureExceptions().isEmpty()) {
                            log.warn("Report generation step completed with {} failures",
                                    stepExecution.getFailureExceptions().size());
                        }
                        return org.springframework.batch.core.ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    @Bean
    public Step workflowCleanupStep() {
        return new StepBuilder("workflowCleanupStep", jobRepository)
                .tasklet(workflowCleanupTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Job reportGenerationJob() {
        return new JobBuilder("reportGenerationJob", jobRepository)
                .start(reportGenerationStep())
                .listener(completionListener)
                .build();
    }

    @Bean
    public Job workflowCleanupJob() {
        return new JobBuilder("workflowCleanupJob", jobRepository)
                .start(workflowCleanupStep())
                .listener(completionListener)
                .build();
    }

    @Bean
    public Job maintenanceJob() {
        return new JobBuilder("maintenanceJob", jobRepository)
                .start(workflowCleanupStep())
                .next(reportGenerationStep())
                .listener(completionListener)
                .build();
    }
}
