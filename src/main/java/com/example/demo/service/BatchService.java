package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.demo.model.Sale;
import com.example.demo.repository.SaleRepository;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.ExitStatus;

@Service
public class BatchService {

    private static final Logger logger = LoggerFactory.getLogger(BatchService.class);

    @Value("${APP_NAME:worker}")
    private String role;

    private final JobLauncher jobLauncher;
    private final Job salesInventoryJob;
    private final SaleRepository saleRepository;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final JobRepository jobRepository;

    public BatchService(JobLauncher jobLauncher, Job salesInventoryJob,
            SaleRepository saleRepository,
            JobExplorer jobExplorer, JobOperator jobOperator, JobRepository jobRepository) {
        this.jobLauncher = jobLauncher;
        this.salesInventoryJob = salesInventoryJob;
        this.saleRepository = saleRepository;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
    }

    public void processAllAtOnce() {
        var allSales = saleRepository.findAll();
        for (Sale sale : allSales) {
            sale.setProcessed(true);
            saleRepository.save(sale);
        }
        logger.info("[ALL-AT-ONCE] Processed: {} records", allSales.size());
    }

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "daily-sales-job-scheduler", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void runBatchJob() {
        // Some metrics for seventh requirment.
        LocalDate salesDate = LocalDate.now(Clock.systemDefaultZone()).minusDays(1);
        logger.info("Instance {} acquired scheduler lock for DailySalesJob, salesDate={}", role, salesDate);
        // logger.info("Instance {} Srart DailySalesJob, salesDate={}", role,
        // salesDate);

        processSalesInChunks(salesDate);
    }

    public void processSalesInChunks(LocalDate salesDate) {
        try {
            JobInstance lastInstance = jobExplorer.getLastJobInstance("salesInventoryJob");

            if (lastInstance != null) {
                JobExecution lastExecution = jobExplorer.getLastJobExecution(lastInstance);
                BatchStatus status = lastExecution.getStatus();

                if (status == BatchStatus.STARTED || status == BatchStatus.STARTING) {
                    logger.warn("Job stuck in STARTED (likely container crash). Forcing FAILED...");

                    for (var stepExecution : lastExecution.getStepExecutions()) {
                        if (stepExecution.getStatus() == BatchStatus.STARTED ||
                                stepExecution.getStatus() == BatchStatus.STARTING) {
                            stepExecution.setStatus(BatchStatus.FAILED);
                            stepExecution.setExitStatus(ExitStatus.FAILED);
                            jobRepository.update(stepExecution);
                        }
                    }

                    lastExecution.setStatus(BatchStatus.FAILED);
                    lastExecution.setExitStatus(ExitStatus.FAILED);
                    jobRepository.update(lastExecution);

                    logger.info("Marked execution {} as FAILED. Now restarting...", lastExecution.getId());
                    jobOperator.restart(lastExecution.getId());
                    return;
                }

                if (status == BatchStatus.FAILED) {
                    logger.info("Restarting FAILED job execution: {}", lastExecution.getId());
                    jobOperator.restart(lastExecution.getId());
                    return;
                }
            }

            JobParameters params = new JobParametersBuilder()
                    .addString("salesDate", salesDate.toString()) // We just added this parameter for seventh req.
                    .addLong("startTime", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(salesInventoryJob, params);
            logger.info("[JOB] Status: {} | ID: {}", execution.getStatus(), execution.getJobId());

        } catch (Exception e) {
            logger.error("[JOB] Failed: {}", e.getMessage(), e);
        }
    }
}
